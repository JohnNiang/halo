package run.halo.app.extension.index;

public class LongIndexEntry extends IndexEntryImpl<Long> {

    /**
     * Creates a new {@link IndexEntryImpl} for the given {@link IndexDescriptor}.
     *
     * @param indexDescriptor for which the {@link IndexEntryImpl} is created.
     */
    public LongIndexEntry(IndexDescriptor<Long> indexDescriptor) {
        super(indexDescriptor);
    }

}
