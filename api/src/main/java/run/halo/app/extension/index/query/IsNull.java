package run.halo.app.extension.index.query;

import java.util.Map;
import java.util.NavigableSet;
import org.springframework.data.relational.core.query.Criteria;

public class IsNull extends SimpleQuery {

    protected IsNull(String fieldName) {
        super(fieldName, null);
    }

    @Override
    public NavigableSet<String> matches(QueryIndexView indexView) {
        indexView.acquireReadLock();
        try {
            var allIds = indexView.getAllIds();
            var idsForNonNullValue = indexView.getIdsForField(fieldName);
            allIds.removeAll(idsForNonNullValue);
            return allIds;
        } finally {
            indexView.releaseReadLock();
        }
    }

    @Override
    public Criteria toCriteria(Map<String, String> fieldNameMap) {
        var columnName = fieldNameMap.getOrDefault(this.fieldName, this.fieldName);
        return Criteria.where(columnName).isNull();
    }

    @Override
    public String toString() {
        return fieldName + " IS NULL";
    }
}
