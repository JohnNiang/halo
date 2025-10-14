package run.halo.app.extension.indexer;

import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import run.halo.app.extension.Extension;
import run.halo.app.extension.ExtensionConverter;
import run.halo.app.extension.ExtensionStoreUtil;
import run.halo.app.extension.Scheme;
import run.halo.app.extension.event.SchemeAddedEvent;
import run.halo.app.extension.index.IndicesInitializer;
import run.halo.app.extension.index.IndicesManager;
import run.halo.app.extension.store.ExtensionStore;
import run.halo.app.extension.store.ExtensionStoreClient;

@Component
@Slf4j
class DefaultIndicesInitializer implements IndicesInitializer {

    private final IndicesManager indicesManager;

    private final ExtensionStoreClient client;

    private final ExtensionConverter extensionConverter;

    DefaultIndicesInitializer(IndicesManager indicesManager,
        ExtensionStoreClient client,
        ExtensionConverter extensionConverter) {
        this.indicesManager = indicesManager;
        this.client = client;
        this.extensionConverter = extensionConverter;
    }

    @EventListener
    void onSchemeAddedEvent(SchemeAddedEvent event) {
        var scheme = event.getScheme();
        this.initialize(scheme);
    }

    @Override
    public void initialize(Scheme scheme) {
        doInitialize(scheme);
    }

    public <E extends Extension> void doInitialize(Scheme scheme) {
        var type = (Class<E>) scheme.type();
        var indices = indicesManager.get(type);
        var prefix = ExtensionStoreUtil.buildStoreNamePrefix(scheme);
        List<ExtensionStore> extensionStores;
        String nameCursor = null;
        var watch = new StopWatch("Initialize indices for " + type.getName());
        var indexedCount = 0L;
        do {
            watch.start("Indexing from " + (nameCursor == null ? "@start" : nameCursor));
            extensionStores = client.listBy(prefix, nameCursor, 100);
            extensionStores.forEach(extensionStore -> {
                var extension = this.extensionConverter.convertFrom(type, extensionStore);
                indices.insert(extension);
            });
            if (!extensionStores.isEmpty()) {
                nameCursor = extensionStores.getLast().getName();
            }
            indexedCount += extensionStores.size();
            watch.stop();
        } while (!Thread.currentThread().isInterrupted() && !extensionStores.isEmpty());
        log.info("Total indexed count: {}, initialization summary: {}",
            indexedCount, watch.prettyPrint(TimeUnit.MILLISECONDS));
    }

}
