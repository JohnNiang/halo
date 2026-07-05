package run.halo.app.extension.materialized;

import java.util.Map;
import org.springframework.data.relational.core.query.Criteria;
import run.halo.app.extension.index.query.AllCondition;
import run.halo.app.extension.index.query.AndCondition;
import run.halo.app.extension.index.query.BetweenCondition;
import run.halo.app.extension.index.query.Condition;
import run.halo.app.extension.index.query.EmptyCondition;
import run.halo.app.extension.index.query.EqualCondition;
import run.halo.app.extension.index.query.GreaterThanCondition;
import run.halo.app.extension.index.query.InCondition;
import run.halo.app.extension.index.query.IndexCondition;
import run.halo.app.extension.index.query.IsNotNullCondition;
import run.halo.app.extension.index.query.IsNullCondition;
import run.halo.app.extension.index.query.LessThanCondition;
import run.halo.app.extension.index.query.NoneCondition;
import run.halo.app.extension.index.query.NotBetweenCondition;
import run.halo.app.extension.index.query.NotCondition;
import run.halo.app.extension.index.query.NotEqualCondition;
import run.halo.app.extension.index.query.NotInCondition;
import run.halo.app.extension.index.query.OrCondition;
import run.halo.app.extension.index.query.StringContainsCondition;
import run.halo.app.extension.index.query.StringEndsWithCondition;
import run.halo.app.extension.index.query.StringNotContainsCondition;
import run.halo.app.extension.index.query.StringNotEndsWithCondition;
import run.halo.app.extension.index.query.StringNotStartsWithCondition;
import run.halo.app.extension.index.query.StringStartsWithCondition;

/**
 * Converts a {@link Condition} tree into a Spring Data {@link Criteria} object.
 *
 * <p>This converter handles field-only conditions (no label/role conditions) and translates them
 * to R2DBC query criteria. The column mapping allows reuse across different extension types.</p>
 *
 * @author johnniang
 * @since 2.22.0
 */
public class ConditionToCriteria {

    private final Map<String, String> columnMapping;

    /**
     * Constructs a new converter with the specified column mapping.
     *
     * @param columnMapping mapping from index names to SQL column names (e.g., "metadata.name" → "name")
     */
    public ConditionToCriteria(Map<String, String> columnMapping) {
        this.columnMapping = Map.copyOf(columnMapping);
    }

    /**
     * Converts a field-only {@link Condition} tree to a {@link Criteria} object.
     *
     * @param condition the condition tree to convert
     * @return the corresponding Criteria object
     * @throws UnsupportedOperationException if the condition type is unknown
     */
    public Criteria convert(Condition condition) {
        return switch (condition) {
            case EmptyCondition ignored -> Criteria.empty();
            case EqualCondition c -> Criteria.where(mapColumn(c.indexName())).is(c.key());
            case NotEqualCondition c -> Criteria.where(mapColumn(c.indexName())).not(c.key());
            case InCondition c -> Criteria.where(mapColumn(c.indexName())).in(c.keys());
            case NotInCondition c -> Criteria.where(mapColumn(c.indexName())).notIn(c.keys());
            case IsNullCondition c -> Criteria.where(mapColumn(c.indexName())).isNull();
            case IsNotNullCondition c -> Criteria.where(mapColumn(c.indexName())).isNotNull();
            case GreaterThanCondition c -> convertGreaterThan(c);
            case LessThanCondition c -> convertLessThan(c);
            case BetweenCondition c -> convertBetween(c);
            case NotBetweenCondition c -> convertNotBetween(c);
            case StringContainsCondition c ->
                Criteria.where(mapColumn(c.indexName())).like("%" + c.keyword() + "%");
            case StringStartsWithCondition c ->
                Criteria.where(mapColumn(c.indexName())).like(c.prefix() + "%");
            case StringEndsWithCondition c ->
                Criteria.where(mapColumn(c.indexName())).like("%" + c.suffix());
            case StringNotContainsCondition c ->
                Criteria.where(mapColumn(c.indexName())).notLike("%" + c.keyword() + "%");
            case StringNotStartsWithCondition c ->
                Criteria.where(mapColumn(c.indexName())).notLike(c.prefix() + "%");
            case StringNotEndsWithCondition c ->
                Criteria.where(mapColumn(c.indexName())).notLike("%" + c.suffix());
            case AndCondition c -> convert(c.left()).and(convert(c.right()));
            case OrCondition c -> convert(c.left()).or(convert(c.right()));
            case NotCondition c -> negate(c.condition());
            case AllCondition ignored -> Criteria.empty();
            case NoneCondition ignored -> createImpossibleCriteria();
            default -> throw new UnsupportedOperationException(
                "Unknown condition type: " + condition.getClass().getName());
        };
    }

    /**
     * Negates a condition by converting it to its inverse Criteria representation.
     *
     * @param condition the condition to negate
     * @return the negated Criteria
     */
    private Criteria negate(Condition condition) {
        return switch (condition) {
            case EmptyCondition ignored -> createImpossibleCriteria();
            case EqualCondition c -> Criteria.where(mapColumn(c.indexName())).not(c.key());
            case NotEqualCondition c -> Criteria.where(mapColumn(c.indexName())).is(c.key());
            case InCondition c -> Criteria.where(mapColumn(c.indexName())).notIn(c.keys());
            case NotInCondition c -> Criteria.where(mapColumn(c.indexName())).in(c.keys());
            case IsNullCondition c -> Criteria.where(mapColumn(c.indexName())).isNotNull();
            case IsNotNullCondition c -> Criteria.where(mapColumn(c.indexName())).isNull();
            case GreaterThanCondition c -> negateGreaterThan(c);
            case LessThanCondition c -> negateLessThan(c);
            case BetweenCondition c -> convert(c.not());
            case NotBetweenCondition c -> convert(c.not());
            case StringContainsCondition c ->
                Criteria.where(mapColumn(c.indexName())).notLike("%" + c.keyword() + "%");
            case StringStartsWithCondition c ->
                Criteria.where(mapColumn(c.indexName())).notLike(c.prefix() + "%");
            case StringEndsWithCondition c ->
                Criteria.where(mapColumn(c.indexName())).notLike("%" + c.suffix());
            case StringNotContainsCondition c ->
                Criteria.where(mapColumn(c.indexName())).like("%" + c.keyword() + "%");
            case StringNotStartsWithCondition c ->
                Criteria.where(mapColumn(c.indexName())).like(c.prefix() + "%");
            case StringNotEndsWithCondition c ->
                Criteria.where(mapColumn(c.indexName())).like("%" + c.suffix());
            // De Morgan's laws
            case AndCondition c -> negate(c.left()).or(negate(c.right()));
            case OrCondition c -> negate(c.left()).and(negate(c.right()));
            // Double negation
            case NotCondition c -> convert(c.condition());
            case AllCondition ignored -> createImpossibleCriteria();
            case NoneCondition ignored -> Criteria.empty();
            default -> throw new UnsupportedOperationException(
                "Unknown condition type: " + condition.getClass().getName());
        };
    }

    private Criteria convertGreaterThan(GreaterThanCondition c) {
        var column = mapColumn(c.indexName());
        if (c.inclusive()) {
            return Criteria.where(column).greaterThanOrEquals(c.lowerBound());
        } else {
            return Criteria.where(column).greaterThan(c.lowerBound());
        }
    }

    private Criteria convertLessThan(LessThanCondition c) {
        var column = mapColumn(c.indexName());
        if (c.inclusive()) {
            return Criteria.where(column).lessThanOrEquals(c.upperBound());
        } else {
            return Criteria.where(column).lessThan(c.upperBound());
        }
    }

    private Criteria convertBetween(BetweenCondition c) {
        var column = mapColumn(c.indexName());
        if (c.fromInclusive() && c.toInclusive()) {
            return Criteria.where(column).between(c.fromKey(), c.toKey());
        } else if (c.fromInclusive()) {
            // from inclusive, to exclusive: >= from AND < to
            return Criteria.where(column).greaterThanOrEquals(c.fromKey())
                .and(Criteria.where(column).lessThan(c.toKey()));
        } else if (c.toInclusive()) {
            // from exclusive, to inclusive: > from AND <= to
            return Criteria.where(column).greaterThan(c.fromKey())
                .and(Criteria.where(column).lessThanOrEquals(c.toKey()));
        } else {
            // both exclusive: > from AND < to
            return Criteria.where(column).greaterThan(c.fromKey())
                .and(Criteria.where(column).lessThan(c.toKey()));
        }
    }

    private Criteria convertNotBetween(NotBetweenCondition c) {
        var column = mapColumn(c.indexName());
        if (c.fromInclusive() && c.toInclusive()) {
            return Criteria.where(column).notBetween(c.fromKey(), c.toKey());
        } else if (c.fromInclusive()) {
            // NOT (>= from AND < to): < from OR >= to
            return Criteria.where(column).lessThan(c.fromKey())
                .or(Criteria.where(column).greaterThanOrEquals(c.toKey()));
        } else if (c.toInclusive()) {
            // NOT (> from AND <= to): <= from OR > to
            return Criteria.where(column).lessThanOrEquals(c.fromKey())
                .or(Criteria.where(column).greaterThan(c.toKey()));
        } else {
            // NOT (> from AND < to): <= from OR >= to
            return Criteria.where(column).lessThanOrEquals(c.fromKey())
                .or(Criteria.where(column).greaterThanOrEquals(c.toKey()));
        }
    }

    private Criteria negateGreaterThan(GreaterThanCondition c) {
        var column = mapColumn(c.indexName());
        // NOT (> bound) = <= bound; NOT (>= bound) = < bound
        if (c.inclusive()) {
            return Criteria.where(column).lessThan(c.lowerBound());
        } else {
            return Criteria.where(column).lessThanOrEquals(c.lowerBound());
        }
    }

    private Criteria negateLessThan(LessThanCondition c) {
        var column = mapColumn(c.indexName());
        // NOT (< bound) = >= bound; NOT (<= bound) = > bound
        if (c.inclusive()) {
            return Criteria.where(column).greaterThan(c.upperBound());
        } else {
            return Criteria.where(column).greaterThanOrEquals(c.upperBound());
        }
    }

    /**
     * Creates an impossible criteria that matches nothing.
     * This is used for NOT of "match all" conditions.
     */
    private static Criteria createImpossibleCriteria() {
        // name IS NULL AND name IS NOT NULL → always false
        return Criteria.where("name").isNull().and(Criteria.where("name").isNotNull());
    }

    private String mapColumn(String indexName) {
        return columnMapping.getOrDefault(indexName, indexName);
    }
}
