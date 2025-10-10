package run.halo.app.extension.index.query;

import java.util.NavigableSet;
import org.springframework.util.Assert;

public class EqualQuery extends SimpleQuery {

    public EqualQuery(String fieldName, String value) {
        super(fieldName, value);
    }

    public EqualQuery(String fieldName, String value, boolean isFieldRef) {
        super(fieldName, value);
        Assert.notNull(value, "Value must not be null, use IsNull or IsNotNull instead");
    }

    @Override
    public NavigableSet<String> matches(QueryIndexView indexView) {
        return indexView.findIds(fieldName, value);
    }

    @Override
    public String toString() {
        return fieldName + " = '" + value + "'";
    }
}
