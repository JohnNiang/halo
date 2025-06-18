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

public class And extends LogicalQuery {

    /**
     * Creates a new And query with the given child queries.
     *
     * @param childQueries The child queries
     */
    public And(Collection<Query> childQueries) {
        super(childQueries);
        if (this.size < 2) {
            throw new IllegalStateException(
                "An 'And' query cannot have fewer than 2 child queries, " + childQueries.size()
                    + " were supplied");
        }
    }

    @Override
    public NavigableSet<String> matches(QueryIndexView indexView) {
        NavigableSet<String> resultSet = null;
        for (Query query : childQueries) {
            NavigableSet<String> currentResult = query.matches(indexView);
            if (resultSet == null) {
                resultSet = Sets.newTreeSet(currentResult);
            } else {
                resultSet.retainAll(currentResult);
            }
        }
        return resultSet == null ? Sets.newTreeSet() : resultSet;
    }

    @Override
    @NonNull
    public Criteria toCriteria(Map<String, String> fieldNameMap) {
        if (childQueries.isEmpty()) {
            return Criteria.empty();
        }
        var criteria = childQueries.stream()
            .map(query -> query.toCriteria(fieldNameMap))
            .toList();
        return Criteria.from(criteria);
    }

    @Override
    public Condition toCondition(Map<String, String> fieldNameMap, TableLike table,
        MutableBindings bindings) {
        return Conditions.nest(childQueries.stream()
            .map(query -> query.toCondition(fieldNameMap, table, bindings))
            .reduce(Condition::and)
            .orElseThrow(() -> new IllegalStateException(
                "At least one child query is required for an 'And' query."
            )));
    }

    @Override
    public String toString() {
        return "(" + childQueries.stream().map(Query::toString)
            .collect(Collectors.joining(" AND ")) + ")";
    }
}
