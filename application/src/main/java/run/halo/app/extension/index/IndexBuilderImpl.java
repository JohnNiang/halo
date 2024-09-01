package run.halo.app.extension.index;

import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.springframework.lang.NonNull;
import run.halo.app.extension.Extension;

public class IndexBuilderImpl implements IndexBuilder {
    private final List<IndexDescriptor<?>> indexDescriptors;
    private final ExtensionIterator<? extends Extension> extensionIterator;

    private final IndexEntryContainer indexEntries = new IndexEntryContainer();

    public static IndexBuilder of(List<IndexDescriptor<?>> indexDescriptors,
        ExtensionIterator<? extends Extension> extensionIterator) {
        return new IndexBuilderImpl(indexDescriptors, extensionIterator);
    }

    IndexBuilderImpl(List<IndexDescriptor<?>> indexDescriptors,
        ExtensionIterator<? extends Extension> extensionIterator) {
        this.indexDescriptors = indexDescriptors;
        this.extensionIterator = extensionIterator;
        indexDescriptors.forEach(indexDescriptor -> {
            var dataType = indexDescriptor.getSpec().getDataType();
            switch (dataType) {
                case BOOL -> indexEntries.add(
                    new BooleanIndexEntry((IndexDescriptor<Boolean>) indexDescriptor)
                );
                case INTEGER -> indexEntries.add(
                    new IntegerIndexEntry((IndexDescriptor<Integer>) indexDescriptor)
                );
                case LONG ->
                    indexEntries.add(new LongIndexEntry((IndexDescriptor<Long>) indexDescriptor)
                    );
                case FLOAT ->
                    indexEntries.add(new FloatIndexEntry((IndexDescriptor<Float>) indexDescriptor)
                    );
                case DOUBLE -> indexEntries.add(
                    new DoubleIndexEntry((IndexDescriptor<Double>) indexDescriptor)
                );
                case INSTANT -> indexEntries.add(new IndexEntryImpl<Instant>(
                    (IndexDescriptor<Instant>) indexDescriptor)
                );
                default -> indexEntries.add(new StringIndexEntry(
                    (IndexDescriptor<String>) indexDescriptor)
                );
            }
        });
    }

    @Override
    public void startBuildingIndex() {
        while (extensionIterator.hasNext()) {
            var extensionRecord = extensionIterator.next();

            indexRecords(extensionRecord);
        }

        for (IndexDescriptor<?> indexDescriptor : indexDescriptors) {
            indexDescriptor.setReady(true);
        }
    }

    @Override
    @NonNull
    public IndexEntryContainer getIndexEntries() {
        for (IndexEntry<?> indexEntry : indexEntries) {
            if (!indexEntry.getIndexDescriptor().isReady()) {
                throw new IllegalStateException(
                    "IndexEntry are not ready yet for index named "
                        + indexEntry.getIndexDescriptor().getSpec().getName());
            }
        }
        return indexEntries;
    }

    private <E extends Extension> void indexRecords(E extension) {
        for (IndexDescriptor<?> indexDescriptor : indexDescriptors) {
            var indexEntry = indexEntries.get(indexDescriptor);
            addEntry(indexEntry, extension);
        }
    }

    private <E extends Extension, T extends Comparable<? super T>> void addEntry(
        IndexEntry<T> indexEntry, E extension) {
        var indexDescriptor = indexEntry.getIndexDescriptor();
        var indexFunc = indexDescriptor.getSpec().getIndexFunc();
        var objectPrimaryKey = PrimaryKeySpecUtils.getObjectPrimaryKey(extension);
        Set<T> indexKeys = indexFunc.getValues(extension);
        indexEntry.addEntry(new LinkedList<>(indexKeys), objectPrimaryKey);
    }
}
