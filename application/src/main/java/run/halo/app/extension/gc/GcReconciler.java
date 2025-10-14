package run.halo.app.extension.gc;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import run.halo.app.extension.Extension;
import run.halo.app.extension.ExtensionClient;
import run.halo.app.extension.ExtensionConverter;
import run.halo.app.extension.SchemeManager;
import run.halo.app.extension.controller.Controller;
import run.halo.app.extension.controller.ControllerBuilder;
import run.halo.app.extension.controller.DefaultController;
import run.halo.app.extension.controller.DefaultQueue;
import run.halo.app.extension.controller.Reconciler;
import run.halo.app.extension.controller.RequestQueue;
import run.halo.app.extension.event.SchemeAddedEvent;
import run.halo.app.extension.index.IndicesManager;
import run.halo.app.extension.store.ExtensionStoreClient;

@Slf4j
@Component
class GcReconciler implements Reconciler<GcRequest> {

    private final ExtensionClient client;

    private final ExtensionStoreClient storeClient;

    private final ExtensionConverter converter;

    private final IndicesManager indicesManager;

    private final SchemeManager schememanager;

    private final RequestQueue<GcRequest> queue;

    private final GcSynchronizer synchronizer;

    GcReconciler(ExtensionClient client,
        ExtensionStoreClient storeClient,
        ExtensionConverter converter,
        SchemeManager schemeManager,
        IndicesManager indicesManager) {
        this.client = client;
        this.storeClient = storeClient;
        this.converter = converter;
        this.indicesManager = indicesManager;
        this.queue = new DefaultQueue<>(Instant::now, Duration.ofMillis(500));
        this.synchronizer = new GcSynchronizer(client, queue, schemeManager);
        this.schememanager = schemeManager;
    }

    @Override
    public Result reconcile(GcRequest request) {
        log.debug("Extension {} is being deleted", request);
        var scheme = schememanager.get(request.gvk());
        client.fetch(scheme.type(), request.name())
            .filter(deletable())
            .ifPresent(this::doDelete);
        return null;
    }

    private <E extends Extension> void doDelete(E extension) {
        var extensionStore = converter.convertTo(extension);
        var indices = indicesManager.get((Class<E>) extension.getClass());
        indices.delete(extension);
        storeClient.delete(extensionStore.getName(), extensionStore.getVersion());
        log.info("Extension {}/{} was deleted", extension.groupVersionKind(), extension);
    }

    @Override
    public Controller setupWith(ControllerBuilder builder) {
        return new DefaultController<>(
            "garbage-collector-controller",
            this,
            queue,
            synchronizer,
            Duration.ofMillis(500),
            Duration.ofSeconds(1000),
            // TODO Make it configurable
            10);
    }

    @EventListener
    void onSchemeAddedEvent(SchemeAddedEvent event) {
        synchronizer.onApplicationEvent(event);
    }

    private Predicate<Extension> deletable() {
        return extension -> CollectionUtils.isEmpty(extension.getMetadata().getFinalizers())
            && extension.getMetadata().getDeletionTimestamp() != null;
    }
}
