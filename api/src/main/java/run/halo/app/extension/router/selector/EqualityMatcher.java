package run.halo.app.extension.router.selector;

import java.util.function.Function;
import java.util.function.Predicate;
import lombok.Getter;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.lang.NonNull;

public class EqualityMatcher implements SelectorMatcher {
    @Getter
    private final Operator operator;
    private final String key;

    @Getter
    private final String value;

    EqualityMatcher(String key, Operator operator, String value) {
        this.key = key;
        this.operator = operator;
        this.value = value;
    }

    /**
     * The "equal" matcher. Matches a label if the label is present and equal.
     *
     * @param key the matching label key
     * @param value the matching label value
     * @return the equality matcher
     */
    public static EqualityMatcher equal(String key, String value) {
        return new EqualityMatcher(key, Operator.EQUAL, value);
    }

    /**
     * The "not equal" matcher. Matches a label if the label is not present or not equal.
     *
     * @param key the matching label key
     * @param value the matching label value
     * @return the equality matcher
     */
    public static EqualityMatcher notEqual(String key, String value) {
        return new EqualityMatcher(key, Operator.NOT_EQUAL, value);
    }

    @Override
    public String toString() {
        return key
            + " "
            + operator.name().toLowerCase()
            + " "
            + value;
    }

    @Override
    public boolean test(String s) {
        return operator.with(value).test(s);
    }

    @Override
    @NonNull
    public Criteria toCriteria() {
        switch (operator) {
            case EQUAL, DOUBLE_EQUAL -> {
                return Criteria.where("labelName").is(key).and("labelValue").is(value);
            }
            case NOT_EQUAL -> {
                return Criteria.empty().and(
                    Criteria.where("labelName").not(key).or("labelValue").not(value)
                );
            }
            default -> {
            }
        }
        return Criteria.empty();
    }

    @Override
    public String getKey() {
        return key;
    }

    public enum Operator {
        EQUAL(arg -> arg::equals),
        DOUBLE_EQUAL(arg -> arg::equals),
        NOT_EQUAL(arg -> v -> !arg.equals(v));

        private final Function<String, Predicate<String>> matcherFunc;

        Operator(Function<String, Predicate<String>> matcherFunc) {
            this.matcherFunc = matcherFunc;
        }

        Predicate<String> with(String value) {
            return matcherFunc.apply(value);
        }
    }
}
