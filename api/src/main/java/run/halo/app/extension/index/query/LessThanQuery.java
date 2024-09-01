package run.halo.app.extension.index.query;

import java.util.NavigableSet;

public class LessThanQuery<T extends Comparable<? super T>> extends SimpleQuery<T> {

    private final boolean orEqual;

    public LessThanQuery(String fieldName, T value, boolean orEqual) {
        this(fieldName, value, orEqual, false);
    }

    public LessThanQuery(String fieldName, T value, boolean orEqual, boolean isFieldRef) {
        super(fieldName, value, isFieldRef);
        this.orEqual = orEqual;
    }

    @Override
    public NavigableSet<String> matches(QueryIndexView indexView) {
        if (isFieldRef && value instanceof String anotherFieldName) {
            return indexView.findMatchingIdsWithSmallerValues(fieldName, anotherFieldName, orEqual);
        }
        return indexView.findIdsLessThan(fieldName, value, orEqual);
    }

    @Override
    public String toString() {
        return fieldName
            + (orEqual ? " <= " : " < ")
            + (isFieldRef ? value : "'" + value + "'");
    }
}
