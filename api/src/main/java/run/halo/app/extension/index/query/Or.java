package run.halo.app.extension.index.query;

import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Map;
import java.util.NavigableSet;
import java.util.stream.Collectors;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.lang.NonNull;

public class Or extends LogicalQuery {

    public Or(Collection<Query> childQueries) {
        super(childQueries);
    }

    @Override
    public NavigableSet<String> matches(QueryIndexView indexView) {
        var resultSet = Sets.<String>newTreeSet();
        for (Query query : childQueries) {
            resultSet.addAll(query.matches(indexView));
        }
        return resultSet;
    }

    @Override
    @NonNull
    public Criteria toCriteria(Map<String, String> fieldNameMap) {
        if (childQueries.isEmpty()) {
            return Criteria.empty();
        }
        var criteria = childQueries.stream()
            .map(query -> query.toCriteria(fieldNameMap))
            .collect(Collectors.toList());
        return Criteria.empty().or(criteria);
    }

    @Override
    public String toString() {
        return "(" + childQueries.stream().map(Query::toString)
            .collect(Collectors.joining(" OR ")) + ")";
    }
}
