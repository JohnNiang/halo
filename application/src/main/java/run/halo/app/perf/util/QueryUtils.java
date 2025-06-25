package run.halo.app.perf.util;

import static org.springframework.data.support.ReactivePageableExecutionUtils.getPage;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.dialect.R2dbcDialect;
import org.springframework.data.relational.core.sql.Condition;
import org.springframework.data.relational.core.sql.Conditions;
import org.springframework.data.relational.core.sql.Functions;
import org.springframework.data.relational.core.sql.OrderByField;
import org.springframework.data.relational.core.sql.SQL;
import org.springframework.data.relational.core.sql.Select;
import org.springframework.data.relational.core.sql.SimpleFunction;
import org.springframework.data.relational.core.sql.Table;
import org.springframework.data.relational.core.sql.TrueCondition;
import org.springframework.r2dbc.core.PreparedOperation;
import org.springframework.r2dbc.core.binding.MutableBindings;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.extension.GroupKind;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.router.selector.SetMatcher;
import run.halo.app.perf.config.HaloPreparedOperation;
import run.halo.app.perf.entity.LabelEntity;

public enum QueryUtils {
    ;

    public static PreparedOperation<Select> prepareSelect(R2dbcEntityTemplate entityTemplate,
        ListOptions listOptions,
        Pageable pageable,
        Map<String, String> fieldMap,
        GroupKind gk,
        Class<?> entityClass) {
        var dataAccessStrategy = entityTemplate.getDataAccessStrategy();
        var entityTable = Table.create(dataAccessStrategy.getTableName(entityClass));
        var dialect = (R2dbcDialect) dataAccessStrategy.getDialect();
        var bindMarkers = dialect.getBindMarkersFactory().create();
        var bindings = new MutableBindings(bindMarkers);
        var labelSelector = listOptions.getLabelSelector();
        var fieldSelector = listOptions.getFieldSelector();

        var orderByFields = remapSort(pageable.getSort(), fieldMap)
            .stream()
            .map(order -> OrderByField.from(
                        entityTable.column(order.getProperty()), order.getDirection()
                    )
                    .withNullHandling(order.getNullHandling())
            )
            .toList();

        Condition whereCondition = TrueCondition.INSTANCE;
        if (fieldSelector != null) {
            var fieldCondition = fieldSelector.query().toCondition(fieldMap, entityTable, bindings);
            whereCondition = whereCondition.and(Conditions.nest(fieldCondition));
        }

        final Select select;
        if (labelSelector != null && !CollectionUtils.isEmpty(labelSelector.getMatchers())) {
            var labelsTable = Table.create(dataAccessStrategy.getTableName(LabelEntity.class));
            var labelsCondition = labelSelector.getMatchers().stream()
                .map(matcher -> matcher.toCondition(labelsTable, bindings))
                .filter(Objects::nonNull)
                .reduce(Condition::and)
                .orElse(null);
            var matcherOpt = labelSelector.getMatchers().stream()
                .filter(matcher -> matcher instanceof SetMatcher)
                .map(matcher -> (SetMatcher) matcher)
                .filter(matcher -> SetMatcher.Operator.NOT_EXISTS.equals(matcher.getOperator()))
                .findFirst();
            if (matcherOpt.isPresent()) {
                var matcher = matcherOpt.get();
                var subselect = Select.builder()
                    .select(labelsTable.column("entity_id"))
                    .from(labelsTable)
                    .where(labelsTable.column("entity_type").isEqualTo(
                        SQL.bindMarker(bindings.bind(gk.toString()).getPlaceholder())
                    ))
                    .and(labelsTable.column("entity_id").isEqualTo(
                        entityTable.column("id")
                    ))
                    .and(labelsTable.column("label_name").isEqualTo(
                        SQL.bindMarker(bindings.bind(matcher.getKey()).getPlaceholder())
                    ))
                    .build(false);
                whereCondition = whereCondition.and(
                    Conditions.notIn(entityTable.column("id"), subselect)
                );
            }

            if (labelsCondition != null) {
                whereCondition = whereCondition.and(Conditions.nest(labelsCondition));
            }

            var builder = Select.builder()
                .select(entityTable.asterisk())
                .from(entityTable)
                .leftOuterJoin(labelsTable)
                .on(entityTable.column("id"))
                .equals(labelsTable.column("entity_id"))
                .and(labelsTable.column("entity_type"))
                .equals(SQL.bindMarker(bindings.bind(gk.toString()).getPlaceholder()));

            if (pageable.isUnpaged()) {
                select = builder.where(whereCondition)
                    .orderBy(orderByFields)
                    .build(true);
            } else {
                select = builder
                    .limitOffset(pageable.getPageSize(), pageable.getOffset())
                    .where(whereCondition)
                    .orderBy(orderByFields)
                    .build(true);
            }
        } else {
            var fromBuilder = Select.builder()
                .select(entityTable.asterisk())
                .from(entityTable);
            if (pageable.isUnpaged()) {
                select = fromBuilder.where(whereCondition)
                    .orderBy(orderByFields)
                    .build(true);
            } else {
                select = fromBuilder
                    .limitOffset(pageable.getPageSize(), pageable.getOffset())
                    .where(whereCondition)
                    .orderBy(orderByFields)
                    .build(true);
            }
        }

        return new HaloPreparedOperation(
            select, dataAccessStrategy.getStatementMapper().getRenderContext(), bindings
        );
    }

    public static PreparedOperation<Select> prepareCount(R2dbcEntityTemplate entityTemplate,
        ListOptions listOptions, Map<String, String> fieldMap, GroupKind gk, Class<?> entityClass) {
        var dataAccessStrategy = entityTemplate.getDataAccessStrategy();
        var entityTable = Table.create(dataAccessStrategy.getTableName(entityClass));
        var distinctId = SimpleFunction.create("DISTINCT", List.of(entityTable.column("id")));
        var dialect = (R2dbcDialect) dataAccessStrategy.getDialect();
        var bindMarkers = dialect.getBindMarkersFactory().create();
        var bindings = new MutableBindings(bindMarkers);
        var labelSelector = listOptions.getLabelSelector();
        var fieldSelector = listOptions.getFieldSelector();

        Condition whereCondition = TrueCondition.INSTANCE;
        if (fieldSelector != null) {
            var fieldCondition = fieldSelector.query().toCondition(fieldMap, entityTable, bindings);
            whereCondition = whereCondition.and(Conditions.nest(fieldCondition));
        }

        final Select select;
        if (labelSelector != null && !CollectionUtils.isEmpty(labelSelector.getMatchers())) {
            var labelsTable = Table.create(dataAccessStrategy.getTableName(LabelEntity.class));
            var labelsCondition = labelSelector.getMatchers().stream()
                .map(matcher -> matcher.toCondition(labelsTable, bindings))
                .filter(Objects::nonNull)
                .reduce(Condition::and)
                .orElse(null);
            if (labelsCondition != null) {
                whereCondition = whereCondition.and(Conditions.nest(labelsCondition));
            }

            var matcherOpt = labelSelector.getMatchers().stream()
                .filter(matcher -> matcher instanceof SetMatcher)
                .map(matcher -> (SetMatcher) matcher)
                .filter(matcher -> SetMatcher.Operator.NOT_EXISTS.equals(matcher.getOperator()))
                .findFirst();
            if (matcherOpt.isPresent()) {
                var matcher = matcherOpt.get();
                var subselect = Select.builder()
                    .select(labelsTable.column("entity_id"))
                    .from(labelsTable)
                    .where(labelsTable.column("entity_type").isEqualTo(
                        SQL.bindMarker(bindings.bind(gk.toString()).getPlaceholder())
                    ))
                    .and(labelsTable.column("entity_id").isEqualTo(
                        entityTable.column("id")
                    ))
                    .and(labelsTable.column("label_name").isEqualTo(
                        SQL.bindMarker(bindings.bind(matcher.getKey()).getPlaceholder())
                    ))
                    .build(false);
                whereCondition = whereCondition.and(
                    Conditions.notIn(entityTable.column("id"), subselect)
                );
            }

            select = Select.builder().select(Functions.count(distinctId))
                .from(entityTable)
                .leftOuterJoin(labelsTable)
                .on(entityTable.column("id"))
                .equals(labelsTable.column("entity_id"))
                .and(labelsTable.column("entity_type"))
                .equals(SQL.bindMarker(bindings.bind(gk.toString()).getPlaceholder()))
                .where(whereCondition)
                .build(true);
        } else {
            select = Select.builder()
                .select(Functions.count(distinctId))
                .from(entityTable)
                .where(whereCondition)
                .build(true);
        }
        return new HaloPreparedOperation(
            select, dataAccessStrategy.getStatementMapper().getRenderContext(), bindings
        );
    }

    public static Sort remapSort(Sort sort, Map<String, String> fieldMap) {
        if (sort == null || sort.isUnsorted()) {
            return Sort.unsorted();
        }
        return Sort.by(sort.stream()
            .map(order -> {
                var mappedProperty = fieldMap.get(order.getProperty());
                if (mappedProperty == null) {
                    return order;
                }
                return order.withProperty(mappedProperty);
            })
            .toList());
    }

    public static <E> Mono<Page<E>> pageBy(R2dbcEntityTemplate entityTemplate,
        ListOptions options,
        Pageable pageable,
        Map<String, String> fieldMap,
        GroupKind gk,
        Class<E> entityClass) {

        var prepareSelect = QueryUtils.prepareSelect(
            entityTemplate, options, pageable, fieldMap, gk, entityClass
        );
        var preparerCount =
            QueryUtils.prepareCount(entityTemplate, options, fieldMap, gk, entityClass);
        var count = entityTemplate.query(preparerCount, entityClass, Long.class).one();
        return entityTemplate.query(prepareSelect, entityClass)
            .all()
            .collectList()
            .flatMap(items -> getPage(items, pageable, count));
    }

    public static <E> Flux<E> findAllBy(R2dbcEntityTemplate entityTemplate,
        ListOptions options,
        Sort sort,
        Map<String, String> fieldMap,
        GroupKind gk,
        Class<E> entityClass) {

        var prepareSelect = QueryUtils.prepareSelect(
            entityTemplate, options, Pageable.unpaged(sort), fieldMap, gk, entityClass
        );
        return entityTemplate.query(prepareSelect, entityClass).all();
    }

}
