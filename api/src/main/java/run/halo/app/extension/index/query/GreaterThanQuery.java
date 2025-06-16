package run.halo.app.extension.index.query;

import java.util.Map;
import java.util.NavigableSet;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.lang.NonNull;

public class GreaterThanQuery extends SimpleQuery {
    private final boolean orEqual;

    public GreaterThanQuery(String fieldName, String value, boolean orEqual) {
        this(fieldName, value, orEqual, false);
    }

    public GreaterThanQuery(String fieldName, String value, boolean orEqual, boolean isFieldRef) {
        super(fieldName, value, isFieldRef);
        this.orEqual = orEqual;
    }

    @Override
    public NavigableSet<String> matches(QueryIndexView indexView) {
        if (isFieldRef) {
            return indexView.findMatchingIdsWithGreaterValues(fieldName, value, orEqual);
        }
        return indexView.findIdsGreaterThan(fieldName, value, orEqual);
    }

    @Override
    @NonNull
    public Criteria toCriteria(Map<String, String> fieldNameMap) {
        var columnName = fieldNameMap.getOrDefault(this.fieldName, this.fieldName);
        var criteriaStep = Criteria.where(columnName);
        return orEqual ? criteriaStep.greaterThanOrEquals(value) : criteriaStep.greaterThan(value);
    }

    @Override
    public String toString() {
        return fieldName
            + (orEqual ? " >= " : " > ")
            + (isFieldRef ? value : "'" + value + "'");
    }
}
