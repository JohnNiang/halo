package run.halo.app.extension.index.query;

import java.util.Map;
import java.util.NavigableSet;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.lang.NonNull;

public class IsNotNull extends SimpleQuery {

    protected IsNotNull(String fieldName) {
        super(fieldName, null);
    }

    @Override
    public NavigableSet<String> matches(QueryIndexView indexView) {
        return indexView.getIdsForField(fieldName);
    }

    @Override
    @NonNull
    public Criteria toCriteria(Map<String, String> fieldNameMap) {
        var columnName = fieldNameMap.getOrDefault(this.fieldName, this.fieldName);
        return Criteria.where(columnName).isNotNull();
    }

    @Override
    public String toString() {
        return fieldName + " IS NOT NULL";
    }
}
