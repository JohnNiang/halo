package run.halo.app.extension.index;

import static run.halo.app.extension.index.IndexAttributeFactory.attribute;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import org.apache.commons.io.IOUtils;
import run.halo.app.extension.Extension;

class DefaultIndicesManager implements IndicesManager {

    private final ConcurrentMap<Class<? extends Extension>, Indices<? extends Extension>>
        indicesMap;

    DefaultIndicesManager() {
        indicesMap = new ConcurrentHashMap<>();
    }

    @Override
    public <E extends Extension> void add(Class<E> type, List<IndexSpec<E, ?>> indexSpecs) {
        indicesMap.computeIfAbsent(type, t -> {
            var finalIndexSpecs = new ArrayList<>(indexSpecs);
            finalIndexSpecs.addAll(createDefaultIndexSpecs(type));
            List<Index<E, ?>> indices = finalIndexSpecs.stream()
                .map(MultiValueIndex::new)
                .map(index -> (Index<E, ?>) index)
                .collect(Collectors.toUnmodifiableList());
            var finalIndices = new ArrayList<Index<E, ?>>(indices.size() + 1);
            finalIndices.addAll(indices);
            finalIndices.add(new LabelIndexImpl<>());
            return new DefaultIndices<>(finalIndices);
        });
    }

    @Override
    public void close() throws IOException {
        IOUtils.close(indicesMap.values().toArray(Indices[]::new));
        indicesMap.clear();
    }

    @Override
    public <E extends Extension> Indices<E> get(Class<E> type) {
        var indices = (Indices<E>) indicesMap.get(type);
        if (indices == null) {
            throw new IllegalArgumentException("No indices found for type: " + type.getName());
        }
        return indices;
    }

    @Override
    public <E extends Extension> void remove(Class<E> type) {
        var indices = indicesMap.remove(type);
        IOUtils.closeQuietly(indices);
    }

    private <E extends Extension> List<IndexSpec<E, ?>> createDefaultIndexSpecs(Class<E> type) {
        var metadataNameSpec = new IndexSpec<E, String>()
            .setName("metadata.name")
            .setUnique(true)
            .setIndexFunc(attribute(type, String.class, e -> e.getMetadata().getName()));
        var creationTimestampSpec = new IndexSpec<E, Instant>()
            .setName("metadata.creationTimestamp")
            .setOrder(IndexSpec.OrderType.DESC)
            .setIndexFunc(
                attribute(type, Instant.class, e -> e.getMetadata().getCreationTimestamp())
            );
        var deletionTimestampSpec = new IndexSpec<E, Instant>()
            .setName("metadata.deletionTimestamp")
            .setOrder(IndexSpec.OrderType.DESC)
            .setIndexFunc(
                attribute(type, Instant.class, e -> e.getMetadata().getDeletionTimestamp())
            );
        return List.of(metadataNameSpec, creationTimestampSpec, deletionTimestampSpec);
    }
}
