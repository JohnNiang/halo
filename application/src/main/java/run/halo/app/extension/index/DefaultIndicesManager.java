package run.halo.app.extension.index;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;
import org.apache.commons.io.IOUtils;
import run.halo.app.extension.Extension;

class DefaultIndicesManager implements IndicesManager {

    private final ConcurrentMap<Class<? extends Extension>, Indices<? extends Extension>>
        indicesMap;

    DefaultIndicesManager() {
        indicesMap = new ConcurrentHashMap<>();
    }

    @Override
    public <E extends Extension> void add(Class<E> type, List<ValueIndexSpec<E, ?>> indexSpecs) {
        indicesMap.computeIfAbsent(type, t -> {
            var indices = new ArrayList<Index<E, ?>>();
            Stream.concat(indexSpecs.stream(), this.<E>createDefaultIndexSpecs().stream())
                .forEach(indexSpec -> {
                    if (indexSpec instanceof MultiValueIndexSpec<E, ?> spec) {
                        indices.add(new MultiValueIndex<>(spec));
                    } else if (indexSpec instanceof SingleValueIndexSpec<E, ?> spec) {
                        indices.add(new SingleValueIndex<>(spec));
                    }
                    // ignore other implementations, should never happen
                });
            indices.add(new LabelIndexImpl<>());
            return new DefaultIndices<>(indices);
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

    private <E extends Extension> List<ValueIndexSpec<E, ?>> createDefaultIndexSpecs() {
        var metadataNameSpec = new SingleValueBuilder<E, String>(
            "metadata.name", e -> e.getMetadata().getName())
            .setUnique(true)
            .setNullable(false)
            .build();
        var creationTimestampSpec = new SingleValueBuilder<E, Instant>(
            "metadata.creationTimestamp", e -> e.getMetadata().getCreationTimestamp())
            .setUnique(false)
            .setNullable(false)
            .build();
        var deletionTimestampSpec = new SingleValueBuilder<E, Instant>(
            "metadata.deletionTimestamp", e -> e.getMetadata().getDeletionTimestamp())
            .setUnique(false)
            .setNullable(true)
            .build();
        return List.of(metadataNameSpec, creationTimestampSpec, deletionTimestampSpec);
    }
}
