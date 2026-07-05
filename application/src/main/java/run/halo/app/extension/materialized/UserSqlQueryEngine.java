package run.halo.app.extension.materialized;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
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
 * @author halo
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
        return resolveNameSet(result.labelConditions(), result.roleConditions())
            .flatMap(optional -> {
                var criteria = buildCriteria(result.fieldCondition(), optional);
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
        return resolveNameSet(result.labelConditions(), result.roleConditions())
            .flatMapMany(optional -> {
                var criteria = buildCriteria(result.fieldCondition(), optional);
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
        return resolveNameSet(result.labelConditions(), result.roleConditions())
            .flatMap(optional -> {
                var criteria = buildCriteria(result.fieldCondition(), optional);
                return entityOperations.count(Query.query(criteria), UserPo.class);
            });
    }

    /**
     * List user names matching the given options, with pagination.
     *
     * @param options list options with label and/or field selectors
     * @param sort    sort order
     * @param page    pagination info (1-based page number)
     * @return mono of list result containing the current page of names and total count
     */
    public Mono<ListResult<String>> listNamesBy(ListOptions options, Sort sort, PageRequest page) {
        var result = conditionExtractor.extract(options.toCondition());
        return resolveNameSet(result.labelConditions(), result.roleConditions())
            .flatMap(optional -> {
                var criteria = buildCriteria(result.fieldCondition(), optional);
                var query = Query.query(criteria).sort(mapSort(sort)).with(toPageable(page));
                var items = entityOperations.select(UserPo.class).matching(query).all()
                    .map(UserPo::getName)
                    .collectList();
                var count = entityOperations.count(Query.query(criteria), UserPo.class);
                return Mono.zip(items, count);
            })
            .map(tuple -> new ListResult<>(page.getPageNumber(), page.getPageSize(),
                tuple.getT2(), tuple.getT1()));
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
        return resolveNameSet(result.labelConditions(), result.roleConditions())
            .flatMapMany(optional -> {
                var criteria = buildCriteria(result.fieldCondition(), optional);
                var query = Query.query(criteria).sort(mapSort(sort));
                return entityOperations.select(UserPo.class).matching(query).all()
                    .map(UserPo::getName);
            });
    }

    /**
     * Holds the resolved name filter with negation info.
     *
     * @param names    store names
     * @param negated  if true, these names should be EXCLUDED (NOT IN); if false, INCLUDED (IN)
     */
    private record NameFilter(Set<String> names, boolean negated) {}

    /** Sentinel indicating no name filter should be applied. */
    private static final NameFilter NO_FILTER = new NameFilter(null, false);

    /**
     * Resolves label and role conditions into a combined name filter.
     * Returns {@link #NO_FILTER} if no label/role conditions were provided (no restriction).
     */
    private Mono<NameFilter> resolveNameSet(
        java.util.List<run.halo.app.extension.index.query.LabelCondition> labelConditions,
        java.util.List<run.halo.app.extension.index.query.IndexCondition> roleConditions
    ) {
        boolean noLabels = labelConditions.isEmpty();
        boolean noRoles = roleConditions.isEmpty();
        if (noLabels && noRoles) {
            return Mono.just(NO_FILTER);
        }
        if (noLabels) {
            return roleConditionResolver.resolve(roleConditions)
                .map(names -> new NameFilter(names, false));
        }
        var labelMono = labelConditionResolver.resolve(labelConditions);
        if (noRoles) {
            return labelMono.map(result ->
                new NameFilter(result.names(), result.negated()));
        }
        return Mono.zip(
            labelMono,
            roleConditionResolver.resolve(roleConditions)
        ).map(tuple -> {
            var labelResult = tuple.getT1();
            var roleNames = tuple.getT2();
            if (labelResult.negated()) {
                // Negated label: exclude those names from role results.
                var result = new HashSet<>(roleNames);
                result.removeAll(labelResult.names());
                return new NameFilter(result, false);
            }
            var result = new HashSet<>(labelResult.names());
            result.retainAll(roleNames);
            return new NameFilter(result, false);
        });
    }

    private Criteria buildCriteria(Condition fieldCondition, NameFilter filter) {
        var criteria = conditionToCriteria.convert(fieldCondition);
        if (filter == null || filter.names() == null) {
            // No label/role conditions — no name filter needed.
            return criteria;
        }
        var nameSet = filter.names();
        if (nameSet.isEmpty()) {
            if (filter.negated()) {
                // Negated with empty set = exclude nothing = no filter.
                return criteria;
            }
            // Positive with empty set = include nothing = exclude all.
            return criteria.and(Criteria.where("name").is("__no_match__"));
        }
        var shortNames = nameSet.stream()
            .map(UserSqlQueryEngine::extractShortName)
            .collect(java.util.stream.Collectors.toSet());
        if (filter.negated()) {
            return criteria.and(Criteria.where("name").notIn(shortNames));
        }
        return criteria.and(Criteria.where("name").in(shortNames));
    }

    /**
     * Extracts the short name from a full store name.
     * For example, "/registry/users/admin" → "admin".
     */
    static String extractShortName(String storeName) {
        int lastSlash = storeName.lastIndexOf('/');
        return lastSlash >= 0 ? storeName.substring(lastSlash + 1) : storeName;
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
