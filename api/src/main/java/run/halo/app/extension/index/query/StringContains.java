package run.halo.app.extension.index.query;

import com.google.common.collect.Sets;
import java.util.Map;
import java.util.NavigableSet;
import org.apache.commons.lang3.StringUtils;
import run.halo.app.extension.index.IndexEntry;

public class StringContains extends SimpleQuery {
    public StringContains(String fieldName, String value) {
        super(fieldName, value);
    }

    @Override
    public NavigableSet<String> matches(QueryIndexView indexView) {
        var resultSet = Sets.<String>newTreeSet();
        IndexEntry<?, ?> indexEntry = indexView.getIndexEntry(fieldName);

        indexEntry.acquireReadLock();
        try {
            for (Map.Entry<?, String> entry : indexEntry.entries()) {
                var fieldValue = entry.getKey();
                if (StringUtils.containsIgnoreCase(fieldValue.toString(), value.toString())) {
                    resultSet.add(entry.getValue());
                }
            }
            return resultSet;
        } finally {
            indexEntry.releaseReadLock();
        }
    }

    @Override
    public String toString() {
        return "contains(" + fieldName + ", '" + value + "')";
    }
}
