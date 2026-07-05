package run.halo.app.extension.materialized;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.Sort;
import org.springframework.data.r2dbc.core.R2dbcEntityOperations;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.User;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.ListResult;
import run.halo.app.extension.Metadata;
import run.halo.app.extension.PageRequest;
import run.halo.app.extension.index.query.Condition;

/**
 * SQL-based query engine for {@link User} that replaces the in-memory IndexEngine.
 *
 * <p>Orchestrates condition extraction, label/role resolution, criteria conversion, and SQL
 * execution via {@link R2dbcEntityOperations}.</p>
 *
 * @author johnniang
 * @since 2.22.0
 */
@Component
public class UserSqlQueryEngine {

    private static final Map<String, String> COLUMN_MAPPING = Map.of(
        "metadata.name", "name",
        "metadata.creationTimestamp", "creationTimestamp",
        "metadata.deletionTimestamp", "deletionTimestamp",
        "spec.displayName", "displayName",
        "spec.email", "email",
        "spec.emailVerified", "emailVerified",
        "spec.disabled", "disabled"
    );

    private final ConditionExtractor conditionExtractor;
    private final ConditionToCriteria conditionToCriteria;
    private final LabelConditionResolver labelConditionResolver;
    private final RoleConditionResolver roleConditionResolver;
    private final R2dbcEntityOperations entityOperations;

    public UserSqlQueryEngine(
        LabelConditionResolver labelConditionResolver,
        RoleConditionResolver roleConditionResolver,
        R2dbcEntityOperations entityOperations
    ) {
        this.conditionExtractor = new ConditionExtractor();
        this.conditionToCriteria = new ConditionToCriteria(COLUMN_MAPPING);
        this.labelConditionResolver = labelConditionResolver;
        this.roleConditionResolver = roleConditionResolver;
        this.entityOperations = entityOperations;
    }

    /**
     * List users matching the given options, with sorting and pagination.
     *
     * @param options list options with label and/or field selectors
     * @param sort    sort order
     * @param page    pagination info (1-based page number)
     * @return mono of list result containing the current page and total count
     */
    public Mono<ListResult<User>> listBy(ListOptions options, Sort sort, PageRequest page) {
        var result = conditionExtractor.extract(options.toCondition());
        return Mono.zip(
                resolveLabelNames(result.labelConditions()),
                resolveRoleNames(result.roleConditions())
            )
            .flatMap(tuple -> {
                var labelNames = tuple.getT1();
                var roleNames = tuple.getT2();
                var nameSet = combineNameSets(labelNames, roleNames);
                var criteria = buildCriteria(result.fieldCondition(), nameSet);
                var query = Query.query(criteria).sort(mapSort(sort)).with(toPageable(page));
                var items = entityOperations.select(UserPo.class).matching(query).all()
                    .map(this::convertUserPoToUser)
                    .collectList();
                var count = entityOperations.count(Query.query(criteria), UserPo.class);
                return Mono.zip(items, count);
            })
            .map(tuple -> new ListResult<>(page.getPageNumber(), page.getPageSize(),
                tuple.getT2(), tuple.getT1()));
    }

    /**
     * List all users matching the given options (no pagination).
     *
     * @param options list options with label and/or field selectors
     * @param sort    sort order
     * @return flux of matching users
     */
    public Flux<User> listAll(ListOptions options, Sort sort) {
        var result = conditionExtractor.extract(options.toCondition());
        return Mono.zip(
                resolveLabelNames(result.labelConditions()),
                resolveRoleNames(result.roleConditions())
            )
            .flatMapMany(tuple -> {
                var labelNames = tuple.getT1();
                var roleNames = tuple.getT2();
                var nameSet = combineNameSets(labelNames, roleNames);
                var criteria = buildCriteria(result.fieldCondition(), nameSet);
                var query = Query.query(criteria).sort(mapSort(sort));
                return entityOperations.select(UserPo.class).matching(query).all()
                    .map(this::convertUserPoToUser);
            });
    }

    /**
     * Count users matching the given options.
     *
     * @param options list options with label and/or field selectors
     * @return mono of count of matching users
     */
    public Mono<Long> countBy(ListOptions options) {
        var result = conditionExtractor.extract(options.toCondition());
        return Mono.zip(
                resolveLabelNames(result.labelConditions()),
                resolveRoleNames(result.roleConditions())
            )
            .flatMap(tuple -> {
                var labelNames = tuple.getT1();
                var roleNames = tuple.getT2();
                var nameSet = combineNameSets(labelNames, roleNames);
                var criteria = buildCriteria(result.fieldCondition(), nameSet);
                return entityOperations.count(Query.query(criteria), UserPo.class);
            });
    }

    /**
     * List user names matching the given options.
     *
     * @param options list options with label and/or field selectors
     * @param sort    sort order
     * @return flux of matching user names
     */
    public Flux<String> listAllNames(ListOptions options, Sort sort) {
        var result = conditionExtractor.extract(options.toCondition());
        return Mono.zip(
                resolveLabelNames(result.labelConditions()),
                resolveRoleNames(result.roleConditions())
            )
            .flatMapMany(tuple -> {
                var labelNames = tuple.getT1();
                var roleNames = tuple.getT2();
                var nameSet = combineNameSets(labelNames, roleNames);
                var criteria = buildCriteria(result.fieldCondition(), nameSet);
                var query = Query.query(criteria).sort(mapSort(sort));
                return entityOperations.select(UserPo.class).matching(query).all()
                    .map(UserPo::getName);
            });
    }

    private Mono<Set<String>> resolveLabelNames(
        java.util.List<run.halo.app.extension.index.query.LabelCondition> conditions
    ) {
        if (conditions.isEmpty()) {
            return Mono.just(Set.of());
        }
        return labelConditionResolver.resolve(conditions);
    }

    private Mono<Set<String>> resolveRoleNames(
        java.util.List<run.halo.app.extension.index.query.IndexCondition> conditions
    ) {
        if (conditions.isEmpty()) {
            return Mono.just(Set.of());
        }
        return roleConditionResolver.resolve(conditions);
    }

    /**
     * Combines label names and role names with AND semantics.
     *
     * <p>Empty sets from the resolvers indicate no conditions were provided (no restriction).
     * Both sets being non-empty means we intersect. Only one non-empty means use that one.</p>
     */
    Set<String> combineNameSets(Set<String> labelNames, Set<String> roleNames) {
        boolean labelEmpty = labelNames.isEmpty();
        boolean roleEmpty = roleNames.isEmpty();
        if (labelEmpty && roleEmpty) {
            return Set.of();
        }
        if (labelEmpty) {
            return roleNames;
        }
        if (roleEmpty) {
            return labelNames;
        }
        var result = new HashSet<>(labelNames);
        result.retainAll(roleNames);
        return result;
    }

    private Criteria buildCriteria(Condition fieldCondition, Set<String> nameSet) {
        var criteria = conditionToCriteria.convert(fieldCondition);
        if (!nameSet.isEmpty()) {
            criteria = criteria.and(Criteria.where("name").in(nameSet));
        }
        return criteria;
    }

    private User convertUserPoToUser(UserPo po) {
        var user = new User();
        var metadata = new Metadata();
        metadata.setName(po.getName());
        metadata.setCreationTimestamp(po.getCreationTimestamp());
        metadata.setDeletionTimestamp(po.getDeletionTimestamp());
        metadata.setVersion(po.getVersion());
        user.setMetadata(metadata);

        var spec = new User.UserSpec();
        spec.setDisplayName(po.getDisplayName());
        spec.setEmail(po.getEmail());
        spec.setEmailVerified(po.isEmailVerified());
        spec.setDisabled(po.isDisabled());
        user.setSpec(spec);

        return user;
    }

    Sort mapSort(Sort sort) {
        if (!sort.isSorted()) {
            return sort;
        }
        var mapped = sort.stream()
            .map(order -> new Sort.Order(order.getDirection(),
                COLUMN_MAPPING.getOrDefault(order.getProperty(), order.getProperty()),
                order.getNullHandling()))
            .toList();
        return Sort.by(mapped);
    }

    private org.springframework.data.domain.PageRequest toPageable(PageRequest page) {
        return org.springframework.data.domain.PageRequest.of(
            page.getPageNumber() - 1,
            page.getPageSize()
        );
    }
}
