package run.halo.app.extension.materialized;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.extension.index.query.InCondition;
import run.halo.app.extension.index.query.IndexCondition;

/**
 * Resolves role-related {@link IndexCondition} instances into sets of matching user store names
 * by querying the {@code user_roles} table.
 *
 * @author halo
 * @since 2.22.0
 */
@Component
public class RoleConditionResolver {

    private final UserRoleRepository roleRepository;

    public RoleConditionResolver(UserRoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    /**
     * Resolve role conditions to matching user store names.
     * Multiple conditions are intersected (AND semantics).
     * Returns empty set if no conditions provided.
     *
     * @param conditions list of role conditions to resolve
     * @return mono of set of matching user store names
     */
    public Mono<Set<String>> resolve(List<IndexCondition> conditions) {
        if (conditions.isEmpty()) {
            return Mono.just(Set.of());
        }
        return Flux.fromIterable(conditions)
            .concatMap(this::resolveSingle)
            .reduce(this::intersect);
    }

    private Mono<Set<String>> resolveSingle(IndexCondition condition) {
        return switch (condition) {
            case InCondition c -> resolveInCondition(c);
            default -> throw new UnsupportedOperationException(
                "Unknown role condition type: " + condition.getClass().getName());
        };
    }

    @SuppressWarnings("unchecked")
    private Mono<Set<String>> resolveInCondition(InCondition c) {
        Collection<String> roleNames = c.keys().stream()
            .map(Object::toString)
            .collect(Collectors.toUnmodifiableSet());
        return roleRepository.findStoreNamesByRoleNameIn(roleNames)
            .collectList()
            .map(HashSet::new);
    }

    private Set<String> intersect(Set<String> a, Set<String> b) {
        var result = new HashSet<>(a);
        result.retainAll(b);
        return result;
    }
}
