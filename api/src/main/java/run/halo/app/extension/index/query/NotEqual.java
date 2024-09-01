package run.halo.app.extension.index.query;

import java.util.NavigableSet;
import org.springframework.util.Assert;

public class NotEqual<T extends Comparable<? super T>> extends SimpleQuery<T> {
    private final EqualQuery<T> equalQuery;

    public NotEqual(String fieldName, T value) {
        this(fieldName, value, false);
    }

    public NotEqual(String fieldName, T value, boolean isFieldRef) {
        super(fieldName, value, isFieldRef);
        Assert.notNull(value, "Value must not be null, use IsNull or IsNotNull instead");
        this.equalQuery = new EqualQuery<>(fieldName, value, isFieldRef);
    }

    @Override
    public NavigableSet<String> matches(QueryIndexView indexView) {
        indexView.acquireReadLock();
        try {
            NavigableSet<String> equalNames = equalQuery.matches(indexView);
            NavigableSet<String> allNames = indexView.getAllIds();
            allNames.removeAll(equalNames);
            return allNames;
        } finally {
            indexView.releaseReadLock();
        }
    }

    @Override
    public String toString() {
        return fieldName + " != " + (isFieldRef ? value : "'" + value + "'");
    }
}
