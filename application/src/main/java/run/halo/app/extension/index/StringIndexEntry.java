package run.halo.app.extension.index;

public class StringIndexEntry extends IndexEntryImpl<String> {

    /**
     * Creates a new {@link IndexEntryImpl} for the given {@link IndexDescriptor}.
     *
     * @param indexDescriptor for which the {@link IndexEntryImpl} is created.
     */
    public StringIndexEntry(IndexDescriptor<String> indexDescriptor) {
        super(indexDescriptor);
    }
}
