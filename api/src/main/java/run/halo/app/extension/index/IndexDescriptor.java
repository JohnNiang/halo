package run.halo.app.extension.index;

import lombok.Data;
import lombok.ToString;
import run.halo.app.extension.Extension;
import run.halo.app.extension.indexer.IndexSpec;

@Data
@ToString(callSuper = true)
public class IndexDescriptor<E extends Extension, K extends Comparable<K>> {

    private final IndexSpec<E, K> spec;

    /**
     * Record whether the index is ready, managed by {@code IndexBuilder}.
     */
    private boolean ready;

    public IndexDescriptor(IndexSpec<E, K> spec) {
        this.spec = spec;
    }
}
