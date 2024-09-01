package run.halo.app.extension.index.query;

import java.util.NavigableSet;

public class GreaterThanQuery<T extends Comparable<? super T>> extends SimpleQuery<T> {
    private final boolean orEqual;

    public GreaterThanQuery(String fieldName, T value, boolean orEqual) {
        this(fieldName, value, orEqual, false);
    }

    public GreaterThanQuery(String fieldName, T value, boolean orEqual, boolean isFieldRef) {
        super(fieldName, value, isFieldRef);
        this.orEqual = orEqual;
    }

    @Override
    public NavigableSet<String> matches(QueryIndexView indexView) {
        if (isFieldRef && value instanceof String anotherFieldName) {
            return indexView.findMatchingIdsWithGreaterValues(fieldName, anotherFieldName, orEqual);
        }
        return indexView.findIdsGreaterThan(fieldName, value, orEqual);
    }

    @Override
    public String toString() {
        return fieldName
            + (orEqual ? " >= " : " > ")
            + (isFieldRef ? value : "'" + value + "'");
    }
}
