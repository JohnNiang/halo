package run.halo.app.extension.router.selector;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import lombok.Getter;
import org.springframework.data.relational.core.sql.BindMarker;
import org.springframework.data.relational.core.sql.Condition;
import org.springframework.data.relational.core.sql.Conditions;
import org.springframework.data.relational.core.sql.SQL;
import org.springframework.data.relational.core.sql.TableLike;
import org.springframework.r2dbc.core.binding.MutableBindings;
import org.springframework.util.Assert;

public class SetMatcher implements SelectorMatcher {
    @Getter
    private final SetMatcher.Operator operator;
    private final String key;

    @Getter
    private final String[] values;

    SetMatcher(String key, SetMatcher.Operator operator) {
        this(key, operator, new String[] {});
    }

    SetMatcher(String key, SetMatcher.Operator operator, String[] values) {
        Assert.hasText(key, "Key must not be empty");
        this.key = key;
        this.operator = operator;
        this.values = values;
    }

    public static SetMatcher in(String key, String... values) {
        Assert.notEmpty(values, "Values must not be empty");
        return new SetMatcher(key, Operator.IN, values);
    }

    public static SetMatcher notIn(String key, String... values) {
        Assert.notEmpty(values, "Values must not be empty");
        return new SetMatcher(key, Operator.NOT_IN, values);
    }

    public static SetMatcher exists(String key) {
        return new SetMatcher(key, Operator.EXISTS);
    }

    public static SetMatcher notExists(String key) {
        return new SetMatcher(key, Operator.NOT_EXISTS);
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public boolean test(String s) {
        return operator.with(values).test(s);
    }

    @Override
    public Condition toCondition(TableLike table, MutableBindings bindings) {
        switch (operator) {
            case IN -> {
                var inValues = Arrays.stream(values)
                    .map(value -> SQL.bindMarker(bindings.bind(value).getPlaceholder()))
                    .toArray(BindMarker[]::new);
                return Conditions.nest(table.column("label_name").isEqualTo(
                        SQL.bindMarker(bindings.bind(key).getPlaceholder())
                    ).and(table.column("label_value").in(inValues))
                );
            }
            case NOT_IN -> {
                var inValues = Arrays.stream(values)
                    .map(value -> SQL.bindMarker(bindings.bind(value).getPlaceholder()))
                    .toArray(BindMarker[]::new);
                return Conditions.nest(table.column("label_name").isEqualTo(
                        SQL.bindMarker(bindings.bind(key).getPlaceholder())
                    ).and(table.column("label_value").notIn(inValues))
                );
            }
            case EXISTS -> {
                return Conditions.nest(table.column("label_name").isEqualTo(
                    SQL.bindMarker(bindings.bind(key).getPlaceholder())
                ));
            }
            case NOT_EXISTS -> {
                return null;
                // return Conditions.nest(table.column("label_name").isNotEqualTo(
                //     SQL.bindMarker(bindings.bind(key).getPlaceholder())
                // ));
            }
            default -> throw new IllegalArgumentException(
                "Cannot build condition for operator: " + operator
            );
        }
    }

    @Override
    public String toString() {
        if (Operator.EXISTS.equals(operator) || Operator.NOT_EXISTS.equals(operator)) {
            return key + " " + operator;
        }
        return key + " " + operator + " (" + String.join(", ", values) + ")";
    }

    public enum Operator {
        IN(values -> v -> contains(values, v)),
        NOT_IN(values -> v -> !contains(values, v)),
        EXISTS(values -> Objects::nonNull),
        NOT_EXISTS(values -> Objects::isNull);

        private final Function<String[], Predicate<String>> matcherFunc;

        Operator(Function<String[], Predicate<String>> matcherFunc) {
            this.matcherFunc = matcherFunc;
        }

        private static boolean contains(String[] strArray, String s) {
            return Arrays.asList(strArray).contains(s);
        }

        Predicate<String> with(String... values) {
            return matcherFunc.apply(values);
        }
    }
}
