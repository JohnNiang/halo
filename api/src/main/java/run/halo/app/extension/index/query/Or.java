package run.halo.app.extension.index.query;

import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Map;
import java.util.NavigableSet;
import java.util.stream.Collectors;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.sql.Condition;
import org.springframework.data.relational.core.sql.Conditions;
import org.springframework.data.relational.core.sql.TableLike;
import org.springframework.lang.NonNull;
import org.springframework.r2dbc.core.binding.MutableBindings;

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
            .reduce(Criteria::or)
            .orElse(Criteria.empty());
        return Criteria.from(criteria);
    }

    @Override
    public Condition toCondition(Map<String, String> fieldNameMap, TableLike table,
        MutableBindings bindings) {
        return Conditions.nest(childQueries.stream()
            .map(query -> query.toCondition(fieldNameMap, table, bindings))
            .reduce(Condition::or)
            .orElseThrow(() -> new IllegalStateException(
                "At least one child query is required for an 'Or' query"
            )));
    }

    @Override
    public String toString() {
        return "(" + childQueries.stream().map(Query::toString)
            .collect(Collectors.joining(" OR ")) + ")";
    }
}
