package run.halo.app.extension.index.query;

import java.util.Map;
import java.util.NavigableSet;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.sql.Condition;
import org.springframework.data.relational.core.sql.SQL;
import org.springframework.data.relational.core.sql.TableLike;
import org.springframework.lang.NonNull;
import org.springframework.r2dbc.core.binding.MutableBindings;

public class LessThanQuery extends SimpleQuery {
    private final boolean orEqual;

    public LessThanQuery(String fieldName, String value, boolean orEqual) {
        this(fieldName, value, orEqual, false);
    }

    public LessThanQuery(String fieldName, String value, boolean orEqual, boolean isFieldRef) {
        super(fieldName, value, isFieldRef);
        this.orEqual = orEqual;
    }

    @Override
    public NavigableSet<String> matches(QueryIndexView indexView) {
        if (isFieldRef) {
            return indexView.findMatchingIdsWithSmallerValues(fieldName, value, orEqual);
        }
        return indexView.findIdsLessThan(fieldName, value, orEqual);
    }

    @Override
    @NonNull
    public Criteria toCriteria(Map<String, String> fieldNameMap) {
        var columnName = fieldNameMap.getOrDefault(this.fieldName, this.fieldName);
        var criteriaStep = Criteria.where(columnName);
        return orEqual ? criteriaStep.lessThanOrEquals(value) : criteriaStep.lessThan(value);
    }

    @Override
    public Condition toCondition(Map<String, String> fieldNameMap, TableLike table,
        MutableBindings bindings) {
        var columnName = fieldNameMap.getOrDefault(this.fieldName, this.fieldName);
        if (orEqual) {
            return table.column(columnName).isLessOrEqualTo(
                SQL.bindMarker(bindings.bind(value).getPlaceholder())
            );
        }
        return table.column(columnName).isLess(
            SQL.bindMarker(bindings.bind(value).getPlaceholder())
        );
    }

    @Override
    public String toString() {
        return fieldName
            + (orEqual ? " <= " : " < ")
            + (isFieldRef ? value : "'" + value + "'");
    }
}
