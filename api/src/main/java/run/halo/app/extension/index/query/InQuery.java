package run.halo.app.extension.index.query;

import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.sql.BindMarker;
import org.springframework.data.relational.core.sql.Condition;
import org.springframework.data.relational.core.sql.SQL;
import org.springframework.data.relational.core.sql.TableLike;
import org.springframework.lang.NonNull;
import org.springframework.r2dbc.core.binding.MutableBindings;
import org.springframework.util.Assert;
import run.halo.app.extension.index.IndexEntryOperatorImpl;

public class InQuery extends SimpleQuery {
    private final Set<String> values;

    public InQuery(String columnName, Set<String> values) {
        super(columnName, null);
        Assert.notEmpty(values, "Values must not be empty");
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
    public Condition toCondition(Map<String, String> fieldNameMap, TableLike table,
        MutableBindings bindings) {
        var columnName = fieldNameMap.getOrDefault(this.fieldName, this.fieldName);

        var inValues = values.stream()
            .map(value -> SQL.bindMarker(bindings.bind(value).getPlaceholder()))
            .toArray(BindMarker[]::new);
        return table.column(columnName).in(inValues);
    }

    @Override
    public String toString() {
        return fieldName + " IN (" + values.stream()
            .map(value -> "'" + value + "'")
            .collect(Collectors.joining(", ")) + ")";
    }
}
