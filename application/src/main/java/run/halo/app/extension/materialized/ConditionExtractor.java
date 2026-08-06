package run.halo.app.extension.materialized;

import java.util.ArrayList;
import java.util.List;
import run.halo.app.extension.index.query.AndCondition;
import run.halo.app.extension.index.query.Condition;
import run.halo.app.extension.index.query.EmptyCondition;
import run.halo.app.extension.index.query.IndexCondition;
import run.halo.app.extension.index.query.LabelCondition;
import run.halo.app.extension.index.query.NotCondition;
import run.halo.app.extension.index.query.OrCondition;

/**
 * Walks a {@link Condition} tree and separates label/role conditions from field conditions.
 *
 * <p>Label conditions ({@link LabelCondition}) and role conditions ({@link IndexCondition} with
 * index name {@code "roles"}) require special SQL handling (subqueries). Field conditions can be
 * directly converted to Spring Data Criteria.</p>
 *
 * <p>This extractor pulls out label and role leaf conditions from the tree and returns the
 * remaining tree with those leaves replaced by {@link EmptyCondition}.</p>
 *
 * @author johnniang
 * @since 2.22.0
 */
public class ConditionExtractor {

    private static final String ROLES_INDEX_NAME = "roles";

    /**
     * Extracts label and role conditions from the given condition tree.
     *
     * @param condition the condition tree to extract from
     * @return the extraction result containing the field-only condition tree, label conditions,
     *     and role conditions
     */
    public Result extract(Condition condition) {
        if (condition == null || condition instanceof EmptyCondition) {
            return new Result(Condition.empty(), List.of(), List.of());
        }
        if (condition instanceof LabelCondition labelCondition) {
            return new Result(Condition.empty(), List.of(labelCondition), List.of());
        }
        if (condition instanceof IndexCondition indexCondition
            && ROLES_INDEX_NAME.equals(indexCondition.indexName())) {
            return new Result(Condition.empty(), List.of(), List.of(indexCondition));
        }
        if (condition instanceof AndCondition andCondition) {
            var leftResult = extract(andCondition.left());
            var rightResult = extract(andCondition.right());
            return new Result(
                new AndCondition(leftResult.fieldCondition, rightResult.fieldCondition),
                concat(leftResult.labelConditions, rightResult.labelConditions),
                concat(leftResult.roleConditions, rightResult.roleConditions)
            );
        }
        if (condition instanceof OrCondition orCondition) {
            // Don't extract label/role from OR branches. If a label/role condition
            // appears in an OR branch and is replaced with EmptyCondition (no-op),
            // the entire OR would match everything. Instead, leave label/role conditions
            // in the field tree; ConditionToCriteria treats them as Criteria.empty() (no-op).
            return new Result(condition, List.of(), List.of());
        }
        if (condition instanceof NotCondition notCondition) {
            var innerResult = extract(notCondition.condition());
            return new Result(
                new NotCondition(innerResult.fieldCondition),
                innerResult.labelConditions,
                innerResult.roleConditions
            );
        }
        // Any other condition (field condition or AllCondition, etc.)
        return new Result(condition, List.of(), List.of());
    }

    private static <T> List<T> concat(List<T> first, List<T> second) {
        var result = new ArrayList<T>(first.size() + second.size());
        result.addAll(first);
        result.addAll(second);
        return List.copyOf(result);
    }

    /**
     * The result of condition extraction.
     *
     * @param fieldCondition the condition tree with label/role leaves replaced by
     *     {@link EmptyCondition}
     * @param labelConditions all extracted {@link LabelCondition} instances
     * @param roleConditions all extracted role {@link IndexCondition} instances
     */
    public record Result(
        Condition fieldCondition,
        List<LabelCondition> labelConditions,
        List<IndexCondition> roleConditions
    ) {}
}
