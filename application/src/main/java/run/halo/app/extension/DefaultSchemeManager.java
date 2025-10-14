package run.halo.app.extension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import run.halo.app.extension.event.SchemeAddedEvent;
import run.halo.app.extension.event.SchemeRemovedEvent;
import run.halo.app.extension.index.IndexSpecs;
import run.halo.app.extension.index.IndexAttributeFactory;
import run.halo.app.extension.index.IndexSpec;
import run.halo.app.extension.index.IndicesManager;

@Component
public class DefaultSchemeManager implements SchemeManager {

    private final List<Scheme> schemes;

    private final IndicesManager indicesManager;

    private final ApplicationEventPublisher eventPublisher;

    public DefaultSchemeManager(IndicesManager indicesManager,
        ApplicationEventPublisher eventPublisher) {
        this.indicesManager = indicesManager;
        this.eventPublisher = eventPublisher;
        // we have to use CopyOnWriteArrayList at here to prevent concurrent modification between
        // registering and listing.
        schemes = new CopyOnWriteArrayList<>();
    }

    @Override
    public <E extends Extension> void register(Class<E> type,
        Consumer<IndexSpecs<E>> specsConsumer) {
        var scheme = Scheme.buildFromType(type);
        if (schemes.contains(scheme)) {
            return;
        }
        var indexSpecs = new DefaultIndexSpecs<E>();
        if (specsConsumer != null) {
            specsConsumer.accept(indexSpecs);
        }
        indicesManager.add(type, indexSpecs.getIndexSpecs());
        schemes.add(scheme);
        eventPublisher.publishEvent(new SchemeAddedEvent(this, scheme));
    }

    @Override
    public void unregister(@NonNull Scheme scheme) {
        if (schemes.contains(scheme)) {
            indicesManager.remove(scheme.type());
            schemes.remove(scheme);
            eventPublisher.publishEvent(new SchemeRemovedEvent(this, scheme));
        }
    }

    @Override
    @NonNull
    public List<Scheme> schemes() {
        return Collections.unmodifiableList(schemes);
    }

    private static class DefaultIndexSpecs<E extends Extension> implements IndexSpecs<E> {

        private final List<IndexSpec<E, ?>> specs;

        private DefaultIndexSpecs() {
            specs = new ArrayList<>();
            addDefaultIndexSpecs();
        }

        @Override
        public <K extends Comparable<K>> void add(IndexSpec<E, K> indexSpec) {
            specs.add(indexSpec);
        }

        @Override
        public List<IndexSpec<E, ?>> getIndexSpecs() {
            return specs;
        }

        private void addDefaultIndexSpecs() {
            var metadataNameSpec = new IndexSpec<E, String>()
                .setName("metadata.name")
                .setUnique(true)
                .setIndexFunc(IndexAttributeFactory.attribute(e -> e.getMetadata().getName()));
            var creationTimestampSpec = new IndexSpec<E, Instant>()
                .setName("metadata.creationTimestamp")
                .setOrder(IndexSpec.OrderType.DESC)
                .setIndexFunc(
                    IndexAttributeFactory.attribute(e -> e.getMetadata().getCreationTimestamp())
                );
            var deletionTimestampSpec = new IndexSpec<E, Instant>()
                .setName("metadata.deletionTimestamp")
                .setOrder(IndexSpec.OrderType.DESC)
                .setIndexFunc(
                    IndexAttributeFactory.attribute(e -> e.getMetadata().getDeletionTimestamp())
                );
            specs.add(metadataNameSpec);
            specs.add(creationTimestampSpec);
            specs.add(deletionTimestampSpec);
        }
    }
}
