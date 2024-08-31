package run.halo.app.extension.index;

import lombok.Data;
import lombok.ToString;

@Data
@ToString(callSuper = true)
public class IndexDescriptor<T extends Comparable<? super T>> {

    private final IndexSpec<T> spec;

    /**
     * Record whether the index is ready, managed by {@code IndexBuilder}.
     */
    private boolean ready;

    public IndexDescriptor(IndexSpec<T> spec) {
        this.spec = spec;
    }

}
