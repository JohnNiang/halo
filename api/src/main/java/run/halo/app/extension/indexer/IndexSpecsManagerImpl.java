package run.halo.app.extension.indexer;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import run.halo.app.extension.Extension;

/**
 * Default implementation of {@link IndexSpecsManager}.
 *
 * @author johnniang
 * @since 2.22.0
 */
class IndexSpecsManagerImpl implements IndexSpecsManager {

    private final Set<IndexSpecs<?>> indices;

    public IndexSpecsManagerImpl() {
        indices = ConcurrentHashMap.newKeySet();
    }

    @Override
    public <E extends Extension> void add(Class<E> type, List<IndexSpec<E, ?>> specs) {
        indices.add(new IndexSpecs<>(type, specs));
    }

    @Override
    public <E extends Extension> void remove(Class<E> type) {
        indices.removeIf(i -> i.type.equals(type));
    }

    @Override
    public <E extends Extension> List<IndexSpec<E, ?>> get(Class<E> type) {
        return indices.stream()
            .filter(i -> Objects.equals(i.type, type))
            .findFirst()
            .map(i -> (IndexSpecs<E>) i)
            .map(i -> i.specs)
            .orElse(List.of());
    }

    private record IndexSpecs<E extends Extension>(Class<E> type, List<IndexSpec<E, ?>> specs) {

    }

}
