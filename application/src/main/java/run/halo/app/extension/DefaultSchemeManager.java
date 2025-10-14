package run.halo.app.extension;

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
import run.halo.app.extension.index.IndexEngine;
import run.halo.app.extension.index.IndexSpec;
import run.halo.app.extension.index.IndexSpecs;

@Component
public class DefaultSchemeManager implements SchemeManager {

    private final List<Scheme> schemes;

    private final IndexEngine indexEngine;

    private final ApplicationEventPublisher eventPublisher;

    public DefaultSchemeManager(IndexEngine indexEngine,
        ApplicationEventPublisher eventPublisher) {
        this.indexEngine = indexEngine;
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
        indexEngine.getIndicesManager().add(type, indexSpecs.getIndexSpecs());
        schemes.add(scheme);
        eventPublisher.publishEvent(new SchemeAddedEvent(this, scheme));
    }

    @Override
    public void unregister(@NonNull Scheme scheme) {
        if (schemes.contains(scheme)) {
            indexEngine.getIndicesManager().remove(scheme.type());
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
        }

        @Override
        public <K extends Comparable<K>> void add(IndexSpec<E, K> indexSpec) {
            specs.add(indexSpec);
        }

        @Override
        public List<IndexSpec<E, ?>> getIndexSpecs() {
            return specs;
        }

    }
}
