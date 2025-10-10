package run.halo.app.extension.indexer;

import java.util.Optional;
import run.halo.app.extension.Extension;

public interface Indices<E extends Extension> {

    void insert(E extension);

    void update(E extension);

    void delete(E extension);

    <K extends Comparable<K>> Optional<Index<E, K>> getIndex(String indexName);

}
