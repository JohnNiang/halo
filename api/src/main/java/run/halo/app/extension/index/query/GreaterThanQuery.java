package run.halo.app.extension.index.query;

import java.util.NavigableSet;

public class GreaterThanQuery extends SimpleQuery {
    private final boolean orEqual;

    public GreaterThanQuery(String fieldName, String value, boolean orEqual) {
        super(fieldName, value);
        this.orEqual = orEqual;
    }

    @Override
    public NavigableSet<String> matches(QueryIndexView indexView) {
        return indexView.findIdsGreaterThan(fieldName, value, orEqual);
    }

    @Override
    public String toString() {
        return fieldName
            + (orEqual ? " >= " : " > ")
            + "'" + value + "'";
    }
}
