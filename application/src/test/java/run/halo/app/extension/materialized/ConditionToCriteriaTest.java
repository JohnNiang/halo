package run.halo.app.extension.materialized;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.relational.core.query.Criteria;
import run.halo.app.extension.index.query.AndCondition;
import run.halo.app.extension.index.query.BetweenCondition;
import run.halo.app.extension.index.query.Condition;
import run.halo.app.extension.index.query.EmptyCondition;
import run.halo.app.extension.index.query.EqualCondition;
import run.halo.app.extension.index.query.GreaterThanCondition;
import run.halo.app.extension.index.query.InCondition;
import run.halo.app.extension.index.query.IsNotNullCondition;
import run.halo.app.extension.index.query.IsNullCondition;
import run.halo.app.extension.index.query.LessThanCondition;
import run.halo.app.extension.index.query.NotBetweenCondition;
import run.halo.app.extension.index.query.NotCondition;
import run.halo.app.extension.index.query.NotEqualCondition;
import run.halo.app.extension.index.query.NotInCondition;
import run.halo.app.extension.index.query.OrCondition;
import run.halo.app.extension.index.query.StringContainsCondition;
import run.halo.app.extension.index.query.StringEndsWithCondition;
import run.halo.app.extension.index.query.StringStartsWithCondition;

/**
 * Tests for {@link ConditionToCriteria}.
 *
 * @author johnniang
 * @since 2.22.0
 */
class ConditionToCriteriaTest {

    private static final Map<String, String> COLUMN_MAPPING = Map.of(
        "metadata.name", "name",
        "metadata.creationTimestamp", "creationTimestamp",
        "metadata.deletionTimestamp", "deletionTimestamp",
        "spec.displayName", "displayName",
        "spec.email", "email",
        "spec.emailVerified", "emailVerified",
        "spec.disabled", "disabled"
    );

    private ConditionToCriteria converter;

    @BeforeEach
    void setUp() {
        converter = new ConditionToCriteria(COLUMN_MAPPING);
    }

    @Test
    void shouldConvertEmptyCondition() {
        var condition = new EmptyCondition();
        var criteria = converter.convert(condition);

        assertThat(criteria).isEqualTo(Criteria.empty());
    }

    @Test
    void shouldConvertEqualCondition() {
        var condition = new EqualCondition("spec.email", "a@b.com");
        var criteria = converter.convert(condition);

        var expected = Criteria.where("email").is("a@b.com");
        assertThat(criteria.toString()).isEqualTo(expected.toString());
    }

    @Test
    void shouldConvertNotEqualCondition() {
        var condition = new NotEqualCondition("spec.disabled", true);
        var criteria = converter.convert(condition);

        var expected = Criteria.where("disabled").not(true);
        assertThat(criteria.toString()).isEqualTo(expected.toString());
    }

    @Test
    void shouldConvertInCondition() {
        var condition = new InCondition("metadata.name", List.of("a", "b"));
        var criteria = converter.convert(condition);

        var expected = Criteria.where("name").in(List.of("a", "b"));
        assertThat(criteria.toString()).isEqualTo(expected.toString());
    }

    @Test
    void shouldConvertNotInCondition() {
        var condition = new NotInCondition("metadata.name", List.of("a", "b"));
        var criteria = converter.convert(condition);

        var expected = Criteria.where("name").notIn(List.of("a", "b"));
        assertThat(criteria.toString()).isEqualTo(expected.toString());
    }

    @Test
    void shouldConvertIsNullCondition() {
        var condition = new IsNullCondition("metadata.deletionTimestamp");
        var criteria = converter.convert(condition);

        var expected = Criteria.where("deletionTimestamp").isNull();
        assertThat(criteria.toString()).isEqualTo(expected.toString());
    }

    @Test
    void shouldConvertIsNotNullCondition() {
        var condition = new IsNotNullCondition("metadata.deletionTimestamp");
        var criteria = converter.convert(condition);

        var expected = Criteria.where("deletionTimestamp").isNotNull();
        assertThat(criteria.toString()).isEqualTo(expected.toString());
    }

    @Test
    void shouldConvertGreaterThanConditionInclusive() {
        var ts = Instant.now();
        var condition = new GreaterThanCondition("metadata.creationTimestamp", ts, true);
        var criteria = converter.convert(condition);

        var expected = Criteria.where("creationTimestamp").greaterThanOrEquals(ts);
        assertThat(criteria.toString()).isEqualTo(expected.toString());
    }

    @Test
    void shouldConvertGreaterThanConditionExclusive() {
        var ts = Instant.now();
        var condition = new GreaterThanCondition("metadata.creationTimestamp", ts, false);
        var criteria = converter.convert(condition);

        var expected = Criteria.where("creationTimestamp").greaterThan(ts);
        assertThat(criteria.toString()).isEqualTo(expected.toString());
    }

    @Test
    void shouldConvertLessThanConditionInclusive() {
        var ts = Instant.now();
        var condition = new LessThanCondition("metadata.creationTimestamp", ts, true);
        var criteria = converter.convert(condition);

        var expected = Criteria.where("creationTimestamp").lessThanOrEquals(ts);
        assertThat(criteria.toString()).isEqualTo(expected.toString());
    }

    @Test
    void shouldConvertLessThanConditionExclusive() {
        var ts = Instant.now();
        var condition = new LessThanCondition("metadata.creationTimestamp", ts, false);
        var criteria = converter.convert(condition);

        var expected = Criteria.where("creationTimestamp").lessThan(ts);
        assertThat(criteria.toString()).isEqualTo(expected.toString());
    }

    @Test
    void shouldConvertBetweenConditionBothInclusive() {
        var from = Instant.parse("2024-01-01T00:00:00Z");
        var to = Instant.parse("2024-12-31T23:59:59Z");
        var condition = new BetweenCondition("metadata.creationTimestamp", from, true, to, true);
        var criteria = converter.convert(condition);

        var expected = Criteria.where("creationTimestamp").between(from, to);
        assertThat(criteria.toString()).isEqualTo(expected.toString());
    }

    @Test
    void shouldConvertBetweenConditionFromInclusiveToExclusive() {
        var from = 1;
        var to = 10;
        var condition = new BetweenCondition("spec.disabled", from, true, to, false);
        var criteria = converter.convert(condition);

        var expected = Criteria.where("disabled").greaterThanOrEquals(from)
            .and(Criteria.where("disabled").lessThan(to));
        assertThat(criteria.toString()).isEqualTo(expected.toString());
    }

    @Test
    void shouldConvertBetweenConditionFromExclusiveToInclusive() {
        var from = 1;
        var to = 10;
        var condition = new BetweenCondition("spec.disabled", from, false, to, true);
        var criteria = converter.convert(condition);

        var expected = Criteria.where("disabled").greaterThan(from)
            .and(Criteria.where("disabled").lessThanOrEquals(to));
        assertThat(criteria.toString()).isEqualTo(expected.toString());
    }

    @Test
    void shouldConvertBetweenConditionBothExclusive() {
        var from = 1;
        var to = 10;
        var condition = new BetweenCondition("spec.disabled", from, false, to, false);
        var criteria = converter.convert(condition);

        var expected = Criteria.where("disabled").greaterThan(from)
            .and(Criteria.where("disabled").lessThan(to));
        assertThat(criteria.toString()).isEqualTo(expected.toString());
    }

    @Test
    void shouldConvertNotBetweenCondition() {
        var from = 1;
        var to = 10;
        var condition = new NotBetweenCondition("spec.disabled", from, true, to, true);
        var criteria = converter.convert(condition);

        var expected = Criteria.where("disabled").notBetween(from, to);
        assertThat(criteria.toString()).isEqualTo(expected.toString());
    }

    @Test
    void shouldConvertStringContainsCondition() {
        var condition = new StringContainsCondition("spec.displayName", "john");
        var criteria = converter.convert(condition);

        var expected = Criteria.where("displayName").like("%john%");
        assertThat(criteria.toString()).isEqualTo(expected.toString());
    }

    @Test
    void shouldConvertStringStartsWithCondition() {
        var condition = new StringStartsWithCondition("spec.displayName", "john");
        var criteria = converter.convert(condition);

        var expected = Criteria.where("displayName").like("john%");
        assertThat(criteria.toString()).isEqualTo(expected.toString());
    }

    @Test
    void shouldConvertStringEndsWithCondition() {
        var condition = new StringEndsWithCondition("spec.displayName", "john");
        var criteria = converter.convert(condition);

        var expected = Criteria.where("displayName").like("%john");
        assertThat(criteria.toString()).isEqualTo(expected.toString());
    }

    @Test
    void shouldConvertAndCondition() {
        var left = new EqualCondition("spec.email", "a@b.com");
        var right = new EqualCondition("spec.disabled", false);
        var condition = new AndCondition(left, right);
        var criteria = converter.convert(condition);

        var expected = Criteria.where("email").is("a@b.com")
            .and(Criteria.where("disabled").is(false));
        assertThat(criteria.toString()).isEqualTo(expected.toString());
    }

    @Test
    void shouldConvertOrCondition() {
        var left = new EqualCondition("spec.email", "a@b.com");
        var right = new EqualCondition("spec.email", "c@d.com");
        var condition = new OrCondition(left, right);
        var criteria = converter.convert(condition);

        var expected = Criteria.where("email").is("a@b.com")
            .or(Criteria.where("email").is("c@d.com"));
        assertThat(criteria.toString()).isEqualTo(expected.toString());
    }

    @Test
    void shouldConvertNotConditionOfEqual() {
        var inner = new EqualCondition("spec.email", "a@b.com");
        var condition = new NotCondition(inner);
        var criteria = converter.convert(condition);

        var expected = Criteria.where("email").not("a@b.com");
        assertThat(criteria.toString()).isEqualTo(expected.toString());
    }

    @Test
    void shouldConvertNotConditionOfNotEqual() {
        var inner = new NotEqualCondition("spec.email", "a@b.com");
        var condition = new NotCondition(inner);
        var criteria = converter.convert(condition);

        var expected = Criteria.where("email").is("a@b.com");
        assertThat(criteria.toString()).isEqualTo(expected.toString());
    }

    @Test
    void shouldConvertNotConditionOfIsNull() {
        var inner = new IsNullCondition("metadata.deletionTimestamp");
        var condition = new NotCondition(inner);
        var criteria = converter.convert(condition);

        var expected = Criteria.where("deletionTimestamp").isNotNull();
        assertThat(criteria.toString()).isEqualTo(expected.toString());
    }

    @Test
    void shouldConvertNotConditionOfIsNotNull() {
        var inner = new IsNotNullCondition("metadata.deletionTimestamp");
        var condition = new NotCondition(inner);
        var criteria = converter.convert(condition);

        var expected = Criteria.where("deletionTimestamp").isNull();
        assertThat(criteria.toString()).isEqualTo(expected.toString());
    }

    @Test
    void shouldConvertNotConditionOfIn() {
        var inner = new InCondition("metadata.name", List.of("a", "b"));
        var condition = new NotCondition(inner);
        var criteria = converter.convert(condition);

        var expected = Criteria.where("name").notIn(List.of("a", "b"));
        assertThat(criteria.toString()).isEqualTo(expected.toString());
    }

    @Test
    void shouldConvertNotConditionOfGreaterThanInclusive() {
        var ts = Instant.now();
        var inner = new GreaterThanCondition("metadata.creationTimestamp", ts, true);
        var condition = new NotCondition(inner);
        var criteria = converter.convert(condition);

        var expected = Criteria.where("creationTimestamp").lessThan(ts);
        assertThat(criteria.toString()).isEqualTo(expected.toString());
    }

    @Test
    void shouldConvertNotConditionOfGreaterThanExclusive() {
        var ts = Instant.now();
        var inner = new GreaterThanCondition("metadata.creationTimestamp", ts, false);
        var condition = new NotCondition(inner);
        var criteria = converter.convert(condition);

        var expected = Criteria.where("creationTimestamp").lessThanOrEquals(ts);
        assertThat(criteria.toString()).isEqualTo(expected.toString());
    }

    @Test
    void shouldConvertNotConditionOfLessThanInclusive() {
        var ts = Instant.now();
        var inner = new LessThanCondition("metadata.creationTimestamp", ts, true);
        var condition = new NotCondition(inner);
        var criteria = converter.convert(condition);

        var expected = Criteria.where("creationTimestamp").greaterThan(ts);
        assertThat(criteria.toString()).isEqualTo(expected.toString());
    }

    @Test
    void shouldConvertNotConditionOfLessThanExclusive() {
        var ts = Instant.now();
        var inner = new LessThanCondition("metadata.creationTimestamp", ts, false);
        var condition = new NotCondition(inner);
        var criteria = converter.convert(condition);

        var expected = Criteria.where("creationTimestamp").greaterThanOrEquals(ts);
        assertThat(criteria.toString()).isEqualTo(expected.toString());
    }

    @Test
    void shouldConvertNotConditionOfBetween() {
        var from = 1;
        var to = 10;
        var inner = new BetweenCondition("spec.disabled", from, true, to, true);
        var condition = new NotCondition(inner);
        var criteria = converter.convert(condition);

        // NOT BETWEEN [from, to] = <= from OR >= to
        var expected = Criteria.where("disabled").lessThanOrEquals(from)
            .or(Criteria.where("disabled").greaterThanOrEquals(to));
        assertThat(criteria.toString()).isEqualTo(expected.toString());
    }

    @Test
    void shouldConvertNotConditionOfBetweenExclusive() {
        var ts1 = Instant.parse("2024-01-01T00:00:00Z");
        var ts2 = Instant.parse("2024-12-31T23:59:59Z");
        var inner = new BetweenCondition("metadata.creationTimestamp", ts1, false, ts2, false);
        var condition = new NotCondition(inner);
        var criteria = converter.convert(condition);

        // NOT (> ts1 AND < ts2) → NotBetweenCondition(ts1, inclusive=true, ts2, inclusive=true)
        // → notBetween(ts1, ts2)
        var expected = Criteria.where("creationTimestamp").notBetween(ts1, ts2);
        assertThat(criteria.toString()).isEqualTo(expected.toString());
    }

    @Test
    void shouldConvertNotConditionOfNotBetween() {
        var from = 1;
        var to = 10;
        var inner = new NotBetweenCondition("spec.disabled", from, true, to, true);
        var condition = new NotCondition(inner);
        var criteria = converter.convert(condition);

        // NOT NOT BETWEEN [from, to] → BetweenCondition(from, exclusive, to, exclusive)
        // = > from AND < to
        var expected = Criteria.where("disabled").greaterThan(from)
            .and(Criteria.where("disabled").lessThan(to));
        assertThat(criteria.toString()).isEqualTo(expected.toString());
    }

    @Test
    void shouldConvertNotConditionOfNotBetweenExclusive() {
        var ts1 = Instant.parse("2024-01-01T00:00:00Z");
        var ts2 = Instant.parse("2024-12-31T23:59:59Z");
        var inner = new NotBetweenCondition("metadata.creationTimestamp", ts1, false, ts2, false);
        var condition = new NotCondition(inner);
        var criteria = converter.convert(condition);

        // NOT NOT BETWEEN (ts1, ts2) → BetweenCondition(ts1, inclusive=true, ts2, inclusive=true)
        // = between(ts1, ts2)
        var expected = Criteria.where("creationTimestamp").between(ts1, ts2);
        assertThat(criteria.toString()).isEqualTo(expected.toString());
    }

    @Test
    void shouldConvertNotConditionOfStringContains() {
        var inner = new StringContainsCondition("spec.displayName", "john");
        var condition = new NotCondition(inner);
        var criteria = converter.convert(condition);

        var expected = Criteria.where("displayName").notLike("%john%");
        assertThat(criteria.toString()).isEqualTo(expected.toString());
    }

    @Test
    void shouldConvertNotConditionOfStringStartsWith() {
        var inner = new StringStartsWithCondition("spec.displayName", "john");
        var condition = new NotCondition(inner);
        var criteria = converter.convert(condition);

        var expected = Criteria.where("displayName").notLike("john%");
        assertThat(criteria.toString()).isEqualTo(expected.toString());
    }

    @Test
    void shouldConvertNotConditionOfStringEndsWith() {
        var inner = new StringEndsWithCondition("spec.displayName", "john");
        var condition = new NotCondition(inner);
        var criteria = converter.convert(condition);

        var expected = Criteria.where("displayName").notLike("%john");
        assertThat(criteria.toString()).isEqualTo(expected.toString());
    }

    @Test
    void shouldApplyDeMorgansLawForNotAndCondition() {
        var left = new EqualCondition("spec.email", "a@b.com");
        var right = new EqualCondition("spec.disabled", false);
        var inner = new AndCondition(left, right);
        var condition = new NotCondition(inner);
        var criteria = converter.convert(condition);

        // NOT (A AND B) = NOT A OR NOT B
        var expected = Criteria.where("email").not("a@b.com")
            .or(Criteria.where("disabled").not(false));
        assertThat(criteria.toString()).isEqualTo(expected.toString());
    }

    @Test
    void shouldApplyDeMorgansLawForNotOrCondition() {
        var left = new EqualCondition("spec.email", "a@b.com");
        var right = new EqualCondition("spec.disabled", false);
        var inner = new OrCondition(left, right);
        var condition = new NotCondition(inner);
        var criteria = converter.convert(condition);

        // NOT (A OR B) = NOT A AND NOT B
        var expected = Criteria.where("email").not("a@b.com")
            .and(Criteria.where("disabled").not(false));
        assertThat(criteria.toString()).isEqualTo(expected.toString());
    }

    @Test
    void shouldConvertDoubleNegation() {
        var inner = new EqualCondition("spec.email", "a@b.com");
        var condition = new NotCondition(new NotCondition(inner));
        var criteria = converter.convert(condition);

        var expected = Criteria.where("email").is("a@b.com");
        assertThat(criteria.toString()).isEqualTo(expected.toString());
    }

    @Test
    void shouldConvertNotConditionOfEmptyCondition() {
        var inner = new EmptyCondition();
        var condition = new NotCondition(inner);
        var criteria = converter.convert(condition);

        // NOT EMPTY = impossible criteria (matches nothing)
        var expected = Criteria.where("name").isNull().and(Criteria.where("name").isNotNull());
        assertThat(criteria.toString()).isEqualTo(expected.toString());
    }

    @Test
    void shouldUseIndexNameAsColumnWhenNoMappingExists() {
        var condition = new EqualCondition("customField", "value");
        var criteria = converter.convert(condition);

        var expected = Criteria.where("customField").is("value");
        assertThat(criteria.toString()).isEqualTo(expected.toString());
    }

    @Test
    void shouldThrowExceptionForUnknownConditionType() {
        var unknownCondition = new Condition() {};

        assertThatThrownBy(() -> converter.convert(unknownCondition))
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessageContaining("Unknown condition type");
    }

    @Test
    void shouldConvertComplexNestedCondition() {
        // (email = "a@b.com" AND disabled = false) OR (displayName CONTAINS "admin")
        var left = new AndCondition(
            new EqualCondition("spec.email", "a@b.com"),
            new EqualCondition("spec.disabled", false)
        );
        var right = new StringContainsCondition("spec.displayName", "admin");
        var condition = new OrCondition(left, right);
        var criteria = converter.convert(condition);

        var expected = Criteria.where("email").is("a@b.com")
            .and(Criteria.where("disabled").is(false))
            .or(Criteria.where("displayName").like("%admin%"));
        assertThat(criteria.toString()).isEqualTo(expected.toString());
    }
}
