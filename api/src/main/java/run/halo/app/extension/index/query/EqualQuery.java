package run.halo.app.extension.index.query;

import java.util.NavigableSet;
import org.springframework.util.Assert;

public class EqualQuery<T extends Comparable<? super T>> extends SimpleQuery<T> {

    public EqualQuery(String fieldName, T value) {
        super(fieldName, value);
    }

    public EqualQuery(String fieldName, T value, boolean isFieldRef) {
        super(fieldName, value, isFieldRef);
        Assert.notNull(value, "Value must not be null, use IsNull or IsNotNull instead");
    }

    @Override
    public NavigableSet<String> matches(QueryIndexView indexView) {
        if (isFieldRef && value instanceof String anotherFieldName) {
            return indexView.findMatchingIdsWithEqualValues(fieldName, anotherFieldName);
        }
        return indexView.findIds(fieldName, value);
    }

    @Override
    public String toString() {
        if (isFieldRef) {
            return fieldName + " = " + value;
        }
        return fieldName + " = '" + value + "'";
    }
}
