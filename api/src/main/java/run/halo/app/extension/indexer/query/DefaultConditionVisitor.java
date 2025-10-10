package run.halo.app.extension.indexer.query;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.relational.core.sql.Visitable;
import org.springframework.data.relational.core.sql.Visitor;
import org.springframework.lang.NonNull;
import run.halo.app.extension.Extension;
import run.halo.app.extension.indexer.Indices;

class DefaultConditionVisitor<E extends Extension> implements Visitor {

    private final ConversionService conversionService;

    private final Indices<E> indices;

    private final Set<String> result;

    public DefaultConditionVisitor(Indices<E> indices) {
        this.indices = indices;
        this.result = new HashSet<>();
        this.conversionService = ApplicationConversionService.getSharedInstance();
    }

    @Override
    public void enter(Visitable segment) {
        var visitor = new ConditionVisitor();
        segment.visit(visitor);
        result.addAll(visitor.getResult());
    }

    @NonNull
    public Set<String> getResult() {
        return result;
    }

    class ConditionVisitor implements Visitor {

        @Getter
        private final Set<String> result;

        ConditionVisitor() {
            result = new HashSet<>();
        }

        @Override
        public void enter(@NonNull Visitable segment) {
            switch (segment) {
                case AndCondition(Condition left, Condition right) -> {
                    var leftVisitor = new ConditionVisitor();
                    var rightVisitor = new ConditionVisitor();
                    left.visit(leftVisitor);
                    right.visit(rightVisitor);
                    result.addAll(leftVisitor.getResult());
                    result.retainAll(rightVisitor.getResult());
                }
                case OrCondition(Condition left, Condition right) -> {
                    var leftVisitor = new ConditionVisitor();
                    var rightVisitor = new ConditionVisitor();
                    left.visit(leftVisitor);
                    right.visit(rightVisitor);
                    result.addAll(leftVisitor.getResult());
                    result.addAll(rightVisitor.getResult());
                }
                case NotCondition(Condition condition) -> {
                    var visitor = new ConditionVisitor();
                    condition.not().visit(visitor);
                    result.addAll(visitor.getResult());
                    // reverse condition
                }
                case EqualCondition(String indexName, Object key) ->
                    result.addAll(equalQuery(indexName, key, false));
                case NotEqualCondition(String indexName, Object key) ->
                    result.addAll(equalQuery(indexName, key, true));
                case InCondition(String indexName, Set<Object> keys) ->
                    result.addAll(inQuery(indexName, keys, false));
                case NotInCondition(String indexName, Set<Object> keys) ->
                    result.addAll(inQuery(indexName, keys, true));
                case LessThanCondition(String indexName, Object upperBound, boolean inclusive) ->
                    result.addAll(lessThanQuery(indexName, upperBound, inclusive, false));
                case GreaterThanCondition(String indexName, Object lowerBound, boolean inclusive) ->
                    result.addAll(lessThanQuery(indexName, lowerBound, inclusive, true));
                case BetweenCondition bc -> result.addAll(betweenQuery(
                        bc.indexName(), bc.fromKey(), bc.fromInclusive(), bc.toKey(),
                        bc.toInclusive(), false
                    )
                );
                case NotBetweenCondition nbc -> result.addAll(betweenQuery(
                    nbc.indexName(), nbc.fromKey(), nbc.fromInclusive(), nbc.toKey(),
                    nbc.toInclusive(), true
                ));
                case IsNullCondition(String indexName) ->
                    result.addAll(isNullQuery(indexName, false));
                case IsNotNullCondition(String indexName) ->
                    result.addAll(isNullQuery(indexName, true));
                case StringContainsCondition(String indexName, String keyword) ->
                    result.addAll(stringContainsQuery(indexName, keyword, false));
                case StringNotContainsCondition(String indexName, String keyword) ->
                    result.addAll(stringContainsQuery(indexName, keyword, true));
                case StringStartsWithCondition(String indexName, String prefix) ->
                    result.addAll(stringStartsWithQuery(indexName, prefix, false));
                case StringEndsWithCondition(String indexName, String suffix) ->
                    result.addAll(stringStartsWithQuery(indexName, suffix, true));
                default -> {
                }
            }
        }
    }

    private Set<String> stringStartsWithQuery(String indexName, String prefix, boolean negated) {
        var index = indices.getIndex(indexName).orElseThrow(
            () -> new IllegalArgumentException("Unknown index: " + indexName)
        );
        if (negated) {
            return index.stringEndsWith(prefix);
        }
        return index.stringStartsWith(prefix);
    }

    private Set<String> stringContainsQuery(String indexName, String keyword, boolean negated) {
        var index = indices.getIndex(indexName).orElseThrow(
            () -> new IllegalArgumentException("Unknown index: " + indexName)
        );
        if (negated) {
            return index.stringNotContains(keyword);
        }
        return index.stringContains(keyword);
    }

    private Collection<String> isNullQuery(String indexName, boolean negated) {
        var index = indices.getIndex(indexName).orElseThrow(
            () -> new IllegalArgumentException("Unknown index: " + indexName)
        );
        if (negated) {
            return index.isNotNull();
        }
        return index.isNull();
    }

    private Set<String> betweenQuery(String indexName, Object fromKey, boolean fromInclusive,
        Object toKey, boolean toInclusive, boolean negated) {
        var index = indices.getIndex(indexName).orElseThrow(
            () -> new IllegalArgumentException("Unknown index: " + indexName)
        );
        if (!conversionService.canConvert(fromKey.getClass(), index.getKeyType())) {
            throw new IllegalArgumentException(
                "Cannot convert key: " + fromKey + " to type: " + index.getKeyType()
            );
        }
        if (!conversionService.canConvert(toKey.getClass(), index.getKeyType())) {
            throw new IllegalArgumentException(
                "Cannot convert key: " + toKey + " to type: " + index.getKeyType()
            );
        }
        if (negated) {
            return index.notBetween(
                conversionService.convert(fromKey, index.getKeyType()),
                fromInclusive,
                conversionService.convert(toKey, index.getKeyType()),
                toInclusive
            );
        } else {
            return index.between(
                conversionService.convert(fromKey, index.getKeyType()),
                fromInclusive,
                conversionService.convert(toKey, index.getKeyType()),
                toInclusive
            );
        }
    }

    private Set<String> lessThanQuery(String indexName, Object bound, boolean inclusive,
        boolean negated) {
        var index = indices.getIndex(indexName).orElseThrow(
            () -> new IllegalArgumentException("Unknown index: " + indexName)
        );
        if (!conversionService.canConvert(bound.getClass(), index.getKeyType())) {
            throw new IllegalArgumentException(
                "Cannot convert key: " + bound + " to type: " + index.getKeyType()
            );
        }
        if (negated) {
            return index.greaterThan(
                conversionService.convert(bound, index.getKeyType()), inclusive
            );
        } else {
            return index.lessThan(conversionService.convert(bound, index.getKeyType()), inclusive);
        }
    }

    private Set<String> equalQuery(String indexName, Object key, boolean negated) {
        var index = indices.getIndex(indexName).orElseThrow(
            () -> new IllegalArgumentException("Unknown index: " + indexName)
        );
        if (!conversionService.canConvert(key.getClass(), index.getKeyType())) {
            throw new IllegalArgumentException(
                "Cannot convert key: " + key + " to type: " + index.getKeyType()
            );
        }
        if (negated) {
            return index.notEqual(conversionService.convert(key, index.getKeyType()));
        } else {
            return index.equal(conversionService.convert(key, index.getKeyType()));
        }
    }

    private Set<String> inQuery(String indexName, Set<Object> keys, boolean negated) {
        var index = indices.getIndex(indexName).orElseThrow(
            () -> new IllegalArgumentException("Unknown index: " + indexName)
        );
        var convertedKeys = keys.stream().map(key -> {
            if (!conversionService.canConvert(key.getClass(), index.getKeyType())) {
                throw new IllegalArgumentException(
                    "Cannot convert key: " + key + " to type: " + index.getKeyType()
                );
            }
            return conversionService.convert(key, index.getKeyType());
        }).collect(Collectors.toSet());
        if (negated) {
            return index.notIn(convertedKeys);
        } else {
            return index.in(convertedKeys);
        }

    }
}
