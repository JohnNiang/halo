package run.halo.app.extension.index.query;

import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.lang.NonNull;
import run.halo.app.extension.index.IndexEntryOperatorImpl;

public class InQuery extends SimpleQuery {
    private final Set<String> values;

    public InQuery(String columnName, Set<String> values) {
        super(columnName, null);
        this.values = values;
    }

    @Override
    public NavigableSet<String> matches(QueryIndexView indexView) {
        var indexEntry = indexView.getIndexEntry(fieldName);
        var operator = new IndexEntryOperatorImpl(indexEntry);
        return operator.findIn(values);
    }

    @Override
    @NonNull
    public Criteria toCriteria(Map<String, String> fieldNameMap) {
        var columnName = fieldNameMap.getOrDefault(this.fieldName, this.fieldName);
        if (values == null || values.isEmpty()) {
            return Criteria.empty();
        }
        return Criteria.where(columnName).in(values);
    }

    @Override
    public String toString() {
        return fieldName + " IN (" + values.stream()
            .map(value -> "'" + value + "'")
            .collect(Collectors.joining(", ")) + ")";
    }
}
