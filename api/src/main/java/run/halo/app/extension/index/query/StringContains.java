package run.halo.app.extension.index.query;

import com.google.common.collect.Sets;
import java.util.Map;
import java.util.NavigableSet;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.lang.NonNull;

public class StringContains extends SimpleQuery {
    public StringContains(String fieldName, String value) {
        super(fieldName, value);
    }

    @Override
    public NavigableSet<String> matches(QueryIndexView indexView) {
        var resultSet = Sets.<String>newTreeSet();
        var indexEntry = indexView.getIndexEntry(fieldName);

        indexEntry.acquireReadLock();
        try {
            for (Map.Entry<String, String> entry : indexEntry.entries()) {
                var fieldValue = entry.getKey();
                if (StringUtils.containsIgnoreCase(fieldValue, value)) {
                    resultSet.add(entry.getValue());
                }
            }
            return resultSet;
        } finally {
            indexEntry.releaseReadLock();
        }
    }

    @Override
    @NonNull
    public Criteria toCriteria(Map<String, String> fieldNameMap) {
        if (value == null || value.isBlank()) {
            return Criteria.empty();
        }
        var columnName = fieldNameMap.getOrDefault(this.fieldName, this.fieldName);
        return Criteria.where(columnName).like("%" + value + "%");
    }

    @Override
    public String toString() {
        return "contains(" + fieldName + ", '" + value + "')";
    }
}
