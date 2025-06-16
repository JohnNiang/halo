package run.halo.app.extension.router.selector;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import lombok.Getter;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.lang.NonNull;

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
        this.key = key;
        this.operator = operator;
        this.values = values;
    }

    public static SetMatcher in(String key, String... values) {
        return new SetMatcher(key, Operator.IN, values);
    }

    public static SetMatcher notIn(String key, String... values) {
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
    @NonNull
    public Criteria toCriteria() {
        switch (operator) {
            case IN -> {
                if (values != null && values.length > 0) {
                    return Criteria.where("labelKey").is(key).and("labelValue").in(List.of(values));
                }
            }
            case NOT_IN -> {
                if (values != null && values.length > 0) {
                    return Criteria.where("labelKey").is(key)
                        .and("labelValue").notIn(List.of(values));
                }
            }
            case EXISTS -> {
                return Criteria.where("labelKey").is(key);
            }
            case NOT_EXISTS -> {
                return Criteria.where("labelKey").not(key);
            }
            default -> {
            }
        }
        return Criteria.empty();
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
