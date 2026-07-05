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
     * Resolve label conditions to matching extension store names.
     * Multiple conditions are intersected (AND semantics).
     * Returns empty set if no conditions provided.
     *
     * @param conditions list of label conditions to resolve
     * @return mono of set of matching extension store names
     */
    public Mono<Set<String>> resolve(List<LabelCondition> conditions) {
        if (conditions.isEmpty()) {
            return Mono.just(Set.of());
        }
        return Flux.fromIterable(conditions)
            .concatMap(this::resolveSingle)
            .reduce(this::intersect);
    }

    private Mono<Set<String>> resolveSingle(LabelCondition condition) {
        return switch (condition) {
            case LabelEqualsCondition c -> labelRepository
                .findExtensionNamesByLabelKeyAndLabelValue(c.labelKey(), c.labelValue())
                .collectList()
                .map(HashSet::new);
            case LabelNotEqualsCondition c -> resolveNotEquals(c);
            case LabelExistsCondition c -> labelRepository
                .findExtensionNamesByLabelKey(c.labelKey())
                .collectList()
                .map(HashSet::new);
            case LabelNotExistsCondition ignored ->
                // Cannot be resolved without knowing all extension names.
                // Return empty set; the caller must handle this case.
                Mono.just(Set.of());
            case LabelInCondition c -> Flux.fromIterable(c.labelValues())
                .concatMap(value -> labelRepository
                    .findExtensionNamesByLabelKeyAndLabelValue(c.labelKey(), value))
                .collectList()
                .map(HashSet::new);
            case LabelNotInCondition c -> resolveNotIn(c);
            default -> throw new UnsupportedOperationException(
                "Unknown label condition type: " + condition.getClass().getName());
        };
    }

    private Mono<Set<String>> resolveNotEquals(LabelNotEqualsCondition c) {
        return labelRepository.findExtensionNamesByLabelKey(c.labelKey())
            .collectList()
            .map(HashSet::new)
            .flatMap(allForKey -> labelRepository
                .findExtensionNamesByLabelKeyAndLabelValue(c.labelKey(), c.labelValue())
                .collectList()
                .map(exactMatches -> {
                    var result = new HashSet<>(allForKey);
                    result.removeAll(new HashSet<>(exactMatches));
                    return result;
                })
            );
    }

    private Mono<Set<String>> resolveNotIn(LabelNotInCondition c) {
        return labelRepository.findExtensionNamesByLabelKey(c.labelKey())
            .collectList()
            .map(HashSet::new)
            .flatMap(allForKey -> Flux.fromIterable(c.labelValues())
                .concatMap(value -> labelRepository
                    .findExtensionNamesByLabelKeyAndLabelValue(c.labelKey(), value))
                .collectList()
                .map(excludedNames -> {
                    var result = new HashSet<>(allForKey);
                    result.removeAll(new HashSet<>(excludedNames));
                    return result;
                })
            );
    }

    private Set<String> intersect(Set<String> a, Set<String> b) {
        var result = new HashSet<>(a);
        result.retainAll(b);
        return result;
    }
}
