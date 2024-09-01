package run.halo.app.extension.index;

import java.time.Instant;

public class InstantIndexEntry extends IndexEntryImpl<Instant> {

    /**
     * Creates a new {@link IndexEntryImpl} for the given {@link IndexDescriptor}.
     *
     * @param indexDescriptor for which the {@link IndexEntryImpl} is created.
     */
    public InstantIndexEntry(IndexDescriptor<Instant> indexDescriptor) {
        super(indexDescriptor);
    }

}
