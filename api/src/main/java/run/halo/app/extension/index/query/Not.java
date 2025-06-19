package run.halo.app.extension.index.query;

import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.Map;
import java.util.NavigableSet;
import lombok.Getter;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.sql.Condition;
import org.springframework.data.relational.core.sql.TableLike;
import org.springframework.lang.NonNull;
import org.springframework.r2dbc.core.binding.MutableBindings;

@Getter
public class Not extends LogicalQuery {

    private final Query negatedQuery;

    public Not(Query negatedQuery) {
        super(Collections.singleton(
            requireNonNull(negatedQuery, "The negated query must not be null.")));
        this.negatedQuery = negatedQuery;
    }

    @Override
    public NavigableSet<String> matches(QueryIndexView indexView) {
        var negatedResult = negatedQuery.matches(indexView);
        var allIds = indexView.getAllIds();
        allIds.removeAll(negatedResult);
        return allIds;
    }

    @Override
    @NonNull
    public Criteria toCriteria(Map<String, String> fieldNameMap) {
        throw new UnsupportedOperationException(
            "The 'NOT' query cannot be converted to Criteria directly. "
                + "Consider using a different query structure.");
    }

    @Override
    public Condition toCondition(Map<String, String> fieldNameMap, TableLike table,
        MutableBindings bindings) {
        return negatedQuery.toCondition(fieldNameMap, table, bindings).not();
    }

    @Override
    public String toString() {
        return "NOT (" + negatedQuery + ")";
    }
}
