package run.halo.app.extension.index.query;

import java.util.NavigableSet;
import java.util.Set;
import java.util.stream.Collectors;
import run.halo.app.extension.index.IndexEntryOperatorImpl;

public class InQuery<T extends Comparable<? super T>> extends SimpleQuery<T> {

    private final Set<T> values;

    public InQuery(String columnName, Set<T> values) {
        super(columnName, null);
        this.values = values;
    }

    @Override
    public NavigableSet<String> matches(QueryIndexView indexView) {
        var indexEntry = indexView.<T>getIndexEntry(fieldName);
        var operator = new IndexEntryOperatorImpl<>(indexEntry);
        return operator.findIn(values);
    }

    @Override
    public String toString() {
        return fieldName + " IN (" + values.stream()
            .map(value -> "'" + value + "'")
            .collect(Collectors.joining(", ")) + ")";
    }
}
