package run.halo.app.extension.indexer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.experimental.UtilityClass;
import org.springframework.util.Assert;
import run.halo.app.extension.index.query.All;
import run.halo.app.extension.index.query.And;
import run.halo.app.extension.index.query.Between;
import run.halo.app.extension.index.query.EqualQuery;
import run.halo.app.extension.index.query.GreaterThanQuery;
import run.halo.app.extension.index.query.InQuery;
import run.halo.app.extension.index.query.IsNotNull;
import run.halo.app.extension.index.query.IsNull;
import run.halo.app.extension.index.query.LessThanQuery;
import run.halo.app.extension.index.query.LogicalQuery;
import run.halo.app.extension.index.query.Not;
import run.halo.app.extension.index.query.NotEqual;
import run.halo.app.extension.index.query.Or;
import run.halo.app.extension.index.query.Query;
import run.halo.app.extension.index.query.SimpleQuery;
import run.halo.app.extension.index.query.StringContains;
import run.halo.app.extension.index.query.StringEndsWith;
import run.halo.app.extension.index.query.StringStartsWith;

@UtilityClass
public class QueryFactory {

    public static Query all() {
        return new All("metadata.name");
    }

    public static Query all(String fieldName) {
        return new All(fieldName);
    }

    public static Query isNull(String fieldName) {
        return new IsNull(fieldName);
    }

    public static Query isNotNull(String fieldName) {
        return new IsNotNull(fieldName);
    }

    /**
     * Create a {@link NotEqual} for the given {@code fieldName} and {@code attributeValue}.
     */
    public static Query notEqual(String fieldName, String attributeValue) {
        if (attributeValue == null) {
            return new IsNotNull(fieldName);
        }
        return new NotEqual(fieldName, attributeValue);
    }

    /**
     * Create a {@link EqualQuery} for the given {@code fieldName} and {@code attributeValue}.
     */
    public static Query equal(String fieldName, String attributeValue) {
        if (attributeValue == null) {
            return new IsNull(fieldName);
        }
        return new EqualQuery(fieldName, attributeValue);
    }

    public static Query lessThan(String fieldName, String attributeValue) {
        return new LessThanQuery(fieldName, attributeValue, false);
    }

    public static Query lessThanOrEqual(String fieldName, String attributeValue) {
        return new LessThanQuery(fieldName, attributeValue, true);
    }

    public static Query greaterThan(String fieldName, String attributeValue) {
        return new GreaterThanQuery(fieldName, attributeValue, false);
    }

    public static Query greaterThanOrEqual(String fieldName, String attributeValue) {
        return new GreaterThanQuery(fieldName, attributeValue, true);
    }

    public static Query in(String fieldName, String... attributeValues) {
        return in(fieldName, Set.of(attributeValues));
    }

    /**
     * Create an {@link InQuery} for the given {@code fieldName} and {@code values}.
     */
    public static Query in(String fieldName, Collection<String> values) {
        Assert.notNull(values, "Values must not be null");
        if (values.size() == 1) {
            String singleValue = values.iterator().next();
            return equal(fieldName, singleValue);
        }
        // Copy the values into a Set if necessary...
        var valueSet = (values instanceof Set ? (Set<String>) values
            : new HashSet<>(values));
        return new InQuery(fieldName, valueSet);
    }

    /**
     * Create an {@link And} for the given {@link Query}s.
     */
    public static Query and(Collection<Query> queries) {
        Assert.notEmpty(queries, "Queries must not be empty");
        if (queries.size() == 1) {
            return queries.iterator().next();
        }
        return new And(queries);
    }

    public static And and(Query query1, Query query2) {
        Collection<Query> queries = Arrays.asList(query1, query2);
        return new And(queries);
    }

    /**
     * Create an {@link And} for the given {@link Query}s.
     */
    public static Query and(Query query1, Query query2, Query... additionalQueries) {
        var queries = new ArrayList<Query>(2 + additionalQueries.length);
        queries.add(query1);
        queries.add(query2);
        Collections.addAll(queries, additionalQueries);
        return new And(queries);
    }

    /**
     * Create an {@link And} for the given {@link Query}s.
     */
    public static Query and(Query query1, Query query2, Collection<Query> additionalQueries) {
        var queries = new ArrayList<Query>(2 + additionalQueries.size());
        queries.add(query1);
        queries.add(query2);
        queries.addAll(additionalQueries);
        return new And(queries);
    }

    public static Query or(Query query1, Query query2) {
        Collection<Query> queries = Arrays.asList(query1, query2);
        return new Or(queries);
    }

    /**
     * Create an {@link Or} for the given {@link Query}s.
     */
    public static Query or(Query query1, Query query2, Query... additionalQueries) {
        var queries = new ArrayList<Query>(2 + additionalQueries.length);
        queries.add(query1);
        queries.add(query2);
        Collections.addAll(queries, additionalQueries);
        return new Or(queries);
    }

    /**
     * Create an {@link Or} for the given {@link Query}s.
     */
    public static Query or(Query query1, Query query2, Collection<Query> additionalQueries) {
        var queries = new ArrayList<Query>(2 + additionalQueries.size());
        queries.add(query1);
        queries.add(query2);
        queries.addAll(additionalQueries);
        return new Or(queries);
    }

    public static Query not(Query query) {
        return new Not(query);
    }

    public static Query betweenLowerExclusive(String fieldName, String lowerValue,
        String upperValue) {
        return new Between(fieldName, lowerValue, false, upperValue, true);
    }

    public static Query betweenUpperExclusive(String fieldName, String lowerValue,
        String upperValue) {
        return new Between(fieldName, lowerValue, true, upperValue, false);
    }

    public static Query betweenExclusive(String fieldName, String lowerValue,
        String upperValue) {
        return new Between(fieldName, lowerValue, false, upperValue, false);
    }

    public static Query between(String fieldName, String lowerValue, String upperValue) {
        return new Between(fieldName, lowerValue, true, upperValue, true);
    }

    public static Query startsWith(String fieldName, String value) {
        return new StringStartsWith(fieldName, value);
    }

    public static Query endsWith(String fieldName, String value) {
        return new StringEndsWith(fieldName, value);
    }

    public static Query contains(String fieldName, String value) {
        return new StringContains(fieldName, value);
    }

    /**
     * Get all the field names used in the given query.
     *
     * @param query the query
     * @return the field names used in the given query
     */
    public static List<String> getFieldNamesUsedInQuery(Query query) {
        List<String> fieldNames = new ArrayList<>();

        if (query instanceof SimpleQuery simpleQuery) {
            fieldNames.add(simpleQuery.getFieldName());
        } else if (query instanceof LogicalQuery logicalQuery) {
            for (Query childQuery : logicalQuery.getChildQueries()) {
                fieldNames.addAll(getFieldNamesUsedInQuery(childQuery));
            }
        }
        return fieldNames;
    }
}
