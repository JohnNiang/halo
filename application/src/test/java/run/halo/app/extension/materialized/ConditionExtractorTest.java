package run.halo.app.extension.materialized;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import run.halo.app.extension.index.query.AndCondition;
import run.halo.app.extension.index.query.Condition;
import run.halo.app.extension.index.query.EmptyCondition;
import run.halo.app.extension.index.query.IndexCondition;
import run.halo.app.extension.index.query.LabelCondition;
import run.halo.app.extension.index.query.NotCondition;
import run.halo.app.extension.index.query.Queries;

class ConditionExtractorTest {

    private ConditionExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new ConditionExtractor();
    }

    @Test
    void emptyConditionShouldReturnEmptyResult() {
        var result = extractor.extract(Condition.empty());
        assertInstanceOf(EmptyCondition.class, result.fieldCondition());
        assertTrue(result.labelConditions().isEmpty());
        assertTrue(result.roleConditions().isEmpty());
    }

    @Test
    void nullConditionShouldReturnEmptyResult() {
        var result = extractor.extract(null);
        assertInstanceOf(EmptyCondition.class, result.fieldCondition());
        assertTrue(result.labelConditions().isEmpty());
        assertTrue(result.roleConditions().isEmpty());
    }

    @Test
    void singleFieldConditionShouldReturnSameCondition() {
        var fieldCondition = Queries.equal("spec.email", "a@b.com");
        var result = extractor.extract(fieldCondition);
        assertEquals(fieldCondition, result.fieldCondition());
        assertTrue(result.labelConditions().isEmpty());
        assertTrue(result.roleConditions().isEmpty());
    }

    @Test
    void singleLabelConditionShouldBeExtracted() {
        var labelCondition = Queries.labelEqual("app", "halo");
        var result = extractor.extract(labelCondition);
        assertInstanceOf(EmptyCondition.class, result.fieldCondition());
        assertEquals(1, result.labelConditions().size());
        assertEquals(labelCondition, result.labelConditions().getFirst());
        assertTrue(result.roleConditions().isEmpty());
    }

    @Test
    void singleRoleConditionShouldBeExtracted() {
        var roleCondition = Queries.in("roles", "admin");
        var result = extractor.extract(roleCondition);
        assertInstanceOf(EmptyCondition.class, result.fieldCondition());
        assertTrue(result.labelConditions().isEmpty());
        assertEquals(1, result.roleConditions().size());
        var extractedRole = result.roleConditions().getFirst();
        assertInstanceOf(IndexCondition.class, extractedRole);
        assertEquals("roles", ((IndexCondition) extractedRole).indexName());
    }

    @Test
    void andOfFieldAndLabelShouldExtractLabel() {
        var fieldCondition = Queries.equal("spec.email", "a@b.com");
        var labelCondition = Queries.labelEqual("app", "halo");
        var combined = fieldCondition.and(labelCondition);

        var result = extractor.extract(combined);

        assertInstanceOf(AndCondition.class, result.fieldCondition());
        var andResult = (AndCondition) result.fieldCondition();
        assertEquals(fieldCondition, andResult.left());
        assertInstanceOf(EmptyCondition.class, andResult.right());
        assertEquals(1, result.labelConditions().size());
        assertEquals(labelCondition, result.labelConditions().getFirst());
        assertTrue(result.roleConditions().isEmpty());
    }

    @Test
    void andOfFieldAndRoleShouldExtractRole() {
        var fieldCondition = Queries.equal("spec.email", "a@b.com");
        var roleCondition = Queries.in("roles", "admin");
        var combined = fieldCondition.and(roleCondition);

        var result = extractor.extract(combined);

        assertInstanceOf(AndCondition.class, result.fieldCondition());
        var andResult = (AndCondition) result.fieldCondition();
        assertEquals(fieldCondition, andResult.left());
        assertInstanceOf(EmptyCondition.class, andResult.right());
        assertTrue(result.labelConditions().isEmpty());
        assertEquals(1, result.roleConditions().size());
    }

    @Test
    void andOfLabelAndRoleShouldExtractBoth() {
        var labelCondition = Queries.labelEqual("app", "halo");
        var roleCondition = Queries.in("roles", "admin");
        var combined = labelCondition.and(roleCondition);

        var result = extractor.extract(combined);

        assertInstanceOf(AndCondition.class, result.fieldCondition());
        var andResult = (AndCondition) result.fieldCondition();
        assertInstanceOf(EmptyCondition.class, andResult.left());
        assertInstanceOf(EmptyCondition.class, andResult.right());
        assertEquals(1, result.labelConditions().size());
        assertEquals(labelCondition, result.labelConditions().getFirst());
        assertEquals(1, result.roleConditions().size());
    }

    @Test
    void orOfFieldAndLabelShouldNotExtractLabel() {
        // Label conditions inside OR branches must NOT be extracted.
        // Replacing them with EmptyCondition would make the OR match everything.
        var fieldCondition = Queries.equal("spec.email", "a@b.com");
        var labelCondition = Queries.labelEqual("app", "halo");
        var combined = fieldCondition.or(labelCondition);

        var result = extractor.extract(combined);

        // The entire OR is left unchanged in the field tree
        assertEquals(combined, result.fieldCondition());
        assertTrue(result.labelConditions().isEmpty());
        assertTrue(result.roleConditions().isEmpty());
    }

    @Test
    void notOfLabelShouldExtractLabel() {
        // labelCondition.not() returns LabelNotEqualsCondition (still a LabelCondition leaf)
        var labelCondition = Queries.labelEqual("app", "halo");
        var notCondition = labelCondition.not();

        var result = extractor.extract(notCondition);

        // The negated label is still a LabelCondition, so it's extracted as a label
        assertInstanceOf(EmptyCondition.class, result.fieldCondition());
        assertEquals(1, result.labelConditions().size());
        assertTrue(result.roleConditions().isEmpty());
    }

    @Test
    void complexNestedConditionShouldExtractCorrectly() {
        // AND(OR(field1, label1), role1)
        // The OR is left as-is (label not extracted from OR). Only role1 is extracted.
        var fieldCondition = Queries.equal("spec.email", "a@b.com");
        var labelCondition = Queries.labelEqual("app", "halo");
        var roleCondition = Queries.in("roles", "admin");

        var orCondition = fieldCondition.or(labelCondition);
        var combined = orCondition.and(roleCondition);

        var result = extractor.extract(combined);

        // fieldCondition tree: AND(OR(field1, label1), EMPTY)
        assertInstanceOf(AndCondition.class, result.fieldCondition());
        var andResult = (AndCondition) result.fieldCondition();

        // OR branch is preserved as-is (label not extracted)
        assertEquals(orCondition, andResult.left());
        assertInstanceOf(EmptyCondition.class, andResult.right());

        // Label was NOT extracted (it's inside an OR)
        assertTrue(result.labelConditions().isEmpty());
        // Role was extracted from the AND
        assertEquals(1, result.roleConditions().size());
    }

    @Test
    void deeplyNestedConditionsShouldExtractAll() {
        // AND(AND(field1, label1), OR(label2, role1))
        // label1 extracted from AND; label2 and role1 NOT extracted from OR
        var field1 = Queries.equal("spec.name", "test");
        LabelCondition label1 = Queries.labelEqual("env", "prod");
        LabelCondition label2 = Queries.labelExists("tier");
        var role1 = Queries.in("roles", "editor");

        var innerAnd = field1.and(label1);
        var innerOr = ((Condition) label2).or(role1);
        var combined = innerAnd.and(innerOr);

        var result = extractor.extract(combined);

        // Verify field condition tree structure
        assertInstanceOf(AndCondition.class, result.fieldCondition());
        var topAnd = (AndCondition) result.fieldCondition();

        // Left: AND(field1, EMPTY) — label1 extracted from AND
        assertInstanceOf(AndCondition.class, topAnd.left());
        var leftAnd = (AndCondition) topAnd.left();
        assertEquals(field1, leftAnd.left());
        assertInstanceOf(EmptyCondition.class, leftAnd.right());

        // Right: OR(label2, role1) preserved as-is — nothing extracted from OR
        assertEquals(innerOr, topAnd.right());

        // Verify extracted conditions: only label1 from the AND branch
        assertEquals(1, result.labelConditions().size());
        assertEquals(label1, result.labelConditions().getFirst());
        assertTrue(result.roleConditions().isEmpty());
    }

    @Test
    void fieldConditionsArePreservedInTree() {
        var specCondition = Queries.equal("spec.email", "a@b.com");
        var statusCondition = Queries.equal("status.phase", "Active");
        var combined = specCondition.and(statusCondition);

        var result = extractor.extract(combined);

        assertEquals(combined, result.fieldCondition());
        assertTrue(result.labelConditions().isEmpty());
        assertTrue(result.roleConditions().isEmpty());
    }

    @Test
    void notOfFieldConditionShouldPreserveFieldCondition() {
        var fieldCondition = Queries.equal("spec.disabled", true);
        var notCondition = fieldCondition.not();

        var result = extractor.extract(notCondition);

        assertEquals(notCondition, result.fieldCondition());
        assertTrue(result.labelConditions().isEmpty());
        assertTrue(result.roleConditions().isEmpty());
    }

    @Test
    void labelExistsConditionShouldBeExtracted() {
        var labelExists = Queries.labelExists("app");
        var result = extractor.extract(labelExists);
        assertInstanceOf(EmptyCondition.class, result.fieldCondition());
        assertEquals(1, result.labelConditions().size());
        assertEquals(labelExists, result.labelConditions().getFirst());
        assertTrue(result.roleConditions().isEmpty());
    }

    @Test
    void labelInConditionShouldBeExtracted() {
        var labelIn = Queries.labelIn("env", List.of("prod", "staging"));
        var result = extractor.extract(labelIn);
        assertInstanceOf(EmptyCondition.class, result.fieldCondition());
        assertEquals(1, result.labelConditions().size());
        assertEquals(labelIn, result.labelConditions().getFirst());
        assertTrue(result.roleConditions().isEmpty());
    }

    @Test
    void orOfMultipleFieldsShouldPreserveAll() {
        var field1 = Queries.equal("spec.email", "a@b.com");
        var field2 = Queries.equal("spec.name", "test");
        var combined = Queries.or(field1, field2);

        var result = extractor.extract(combined);

        assertEquals(combined, result.fieldCondition());
        assertTrue(result.labelConditions().isEmpty());
        assertTrue(result.roleConditions().isEmpty());
    }

    @Test
    void notOfRoleShouldExtractRole() {
        // roleCondition.not() returns NotEqualCondition("roles", "admin") (still IndexCondition)
        var roleCondition = Queries.in("roles", "admin");
        var notCondition = roleCondition.not();

        var result = extractor.extract(notCondition);

        // The negated role is still an IndexCondition with indexName "roles", so it's extracted
        assertInstanceOf(EmptyCondition.class, result.fieldCondition());
        assertTrue(result.labelConditions().isEmpty());
        assertEquals(1, result.roleConditions().size());
    }

    @Test
    void explicitNotConditionShouldBeDecomposed() {
        // Constructing a NotCondition explicitly: NOT(field) should preserve field in tree
        var fieldCondition = Queries.equal("spec.email", "a@b.com");
        var notCondition = new NotCondition(fieldCondition);

        var result = extractor.extract(notCondition);

        assertInstanceOf(NotCondition.class, result.fieldCondition());
        var notResult = (NotCondition) result.fieldCondition();
        assertEquals(fieldCondition, notResult.condition());
        assertTrue(result.labelConditions().isEmpty());
        assertTrue(result.roleConditions().isEmpty());
    }

    @Test
    void orOfTwoLabelsShouldNotExtractEither() {
        // OR(label1, label2) — both branches are label conditions;
        // neither should be extracted to avoid the OR-matches-everything bug.
        var label1 = Queries.labelEqual("env", "prod");
        var label2 = Queries.labelExists("tier");
        var combined = ((Condition) label1).or(label2);

        var result = extractor.extract(combined);

        // The entire OR is preserved as-is in the field tree
        assertEquals(combined, result.fieldCondition());
        assertTrue(result.labelConditions().isEmpty());
        assertTrue(result.roleConditions().isEmpty());
    }

    @Test
    void orOfLabelAndRoleShouldNotExtractEither() {
        // OR(label, role) — both are special conditions; leave OR intact.
        var labelCondition = Queries.labelEqual("app", "halo");
        var roleCondition = Queries.in("roles", "admin");
        var combined = ((Condition) labelCondition).or(roleCondition);

        var result = extractor.extract(combined);

        assertEquals(combined, result.fieldCondition());
        assertTrue(result.labelConditions().isEmpty());
        assertTrue(result.roleConditions().isEmpty());
    }
}
