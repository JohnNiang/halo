package run.halo.app.extension.materialized;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.extension.index.query.LabelCondition;
import run.halo.app.extension.index.query.LabelEqualsCondition;
import run.halo.app.extension.index.query.LabelExistsCondition;
import run.halo.app.extension.index.query.LabelInCondition;
import run.halo.app.extension.index.query.LabelNotEqualsCondition;
import run.halo.app.extension.index.query.LabelNotExistsCondition;
import run.halo.app.extension.index.query.LabelNotInCondition;

/**
 * Resolves {@link LabelCondition} instances into sets of matching extension store names
 * by querying the {@code extension_labels} table.
 *
 * @author halo
 * @since 2.22.0
 */
@Component
public class LabelConditionResolver {

    private final ExtensionLabelRepository labelRepository;

    public LabelConditionResolver(ExtensionLabelRepository labelRepository) {
        this.labelRepository = labelRepository;
    }

    /**
     * Result of resolving label conditions.
     *
     * @param names       matching extension store names
     * @param negated     whether the overall condition is negated (true = these are names to EXCLUDE)
     */
    public record ResolutionResult(Set<String> names, boolean negated) {}

    /**
     * Resolve label conditions to a {@link ResolutionResult}.
     * Multiple conditions are intersected (AND semantics).
     * Returns empty (negated=false) if no conditions provided.
     *
     * @param conditions list of label conditions to resolve
     * @return mono of resolution result
     */
    public Mono<ResolutionResult> resolve(List<LabelCondition> conditions) {
        if (conditions.isEmpty()) {
            return Mono.just(new ResolutionResult(Set.of(), false));
        }
        return Flux.fromIterable(conditions)
            .concatMap(this::resolveSingle)
            .reduce(this::combine);
    }

    private Mono<ResolutionResult> resolveSingle(LabelCondition condition) {
        return switch (condition) {
            case LabelEqualsCondition c -> labelRepository
                .findExtensionNamesByLabelKeyAndLabelValue(c.labelKey(), c.labelValue())
                .collectList()
                .map(names -> new ResolutionResult(new HashSet<>(names), false));
            case LabelNotEqualsCondition c -> resolveNotEquals(c);
            case LabelExistsCondition c -> labelRepository
                .findExtensionNamesByLabelKey(c.labelKey())
                .collectList()
                .map(names -> new ResolutionResult(new HashSet<>(names), false));
            case LabelNotExistsCondition ignored ->
                // Cannot be resolved without knowing all extension names.
                Mono.just(new ResolutionResult(Set.of(), false));
            case LabelInCondition c -> Flux.fromIterable(c.labelValues())
                .concatMap(value -> labelRepository
                    .findExtensionNamesByLabelKeyAndLabelValue(c.labelKey(), value))
                .collectList()
                .map(names -> new ResolutionResult(new HashSet<>(names), false));
            case LabelNotInCondition c -> resolveNotIn(c);
            default -> throw new UnsupportedOperationException(
                "Unknown label condition type: " + condition.getClass().getName());
        };
    }

    private Mono<ResolutionResult> resolveNotEquals(LabelNotEqualsCondition c) {
        // Positive: find names that have the label with the given value.
        // These are the names to EXCLUDE.
        return labelRepository
            .findExtensionNamesByLabelKeyAndLabelValue(c.labelKey(), c.labelValue())
            .collectList()
            .map(names -> new ResolutionResult(new HashSet<>(names), true));
    }

    private Mono<ResolutionResult> resolveNotIn(LabelNotInCondition c) {
        // Positive: find names that have the label with any of the given values.
        // These are the names to EXCLUDE.
        return Flux.fromIterable(c.labelValues())
            .concatMap(value -> labelRepository
                .findExtensionNamesByLabelKeyAndLabelValue(c.labelKey(), value))
            .collectList()
            .map(names -> new ResolutionResult(new HashSet<>(names), true));
    }

    private ResolutionResult combine(ResolutionResult a, ResolutionResult b) {
        // Both positive → intersect (AND semantics)
        if (!a.negated() && !b.negated()) {
            var result = new HashSet<>(a.names());
            result.retainAll(b.names());
            return new ResolutionResult(result, false);
        }
        // Both negated → NOT A AND NOT B = NOT (A ∪ B): exclude the union
        if (a.negated() && b.negated()) {
            var combined = new HashSet<>(a.names());
            combined.addAll(b.names());
            return new ResolutionResult(Set.copyOf(combined), true);
        }
        // Mixed: one positive, one negated → positive minus negated
        var positiveNames = a.negated() ? b.names() : a.names();
        var negativeNames = a.negated() ? a.names() : b.names();
        var result = new HashSet<>(positiveNames);
        result.removeAll(negativeNames);
        return new ResolutionResult(result, false);
    }
}
