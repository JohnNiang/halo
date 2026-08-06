package run.halo.app.extension.materialized;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import run.halo.app.extension.index.query.LabelCondition;
import run.halo.app.extension.index.query.LabelEqualsCondition;
import run.halo.app.extension.index.query.LabelExistsCondition;
import run.halo.app.extension.index.query.LabelInCondition;
import run.halo.app.extension.index.query.LabelNotEqualsCondition;
import run.halo.app.extension.index.query.LabelNotExistsCondition;
import run.halo.app.extension.index.query.LabelNotInCondition;

/**
 * Tests for {@link LabelConditionResolver}.
 *
 * @author halo
 * @since 2.22.0
 */
@ExtendWith(MockitoExtension.class)
class LabelConditionResolverTest {

    @Mock
    ExtensionLabelRepository labelRepository;

    LabelConditionResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new LabelConditionResolver(labelRepository);
    }

    @Test
    void shouldReturnEmptyResultForEmptyConditions() {
        resolver.resolve(List.of())
            .as(StepVerifier::create)
            .assertNext(result -> {
                assertThat(result.names()).isEmpty();
                assertThat(result.negated()).isFalse();
            })
            .verifyComplete();
    }

    @Test
    void shouldResolveLabelEqualsCondition() {
        when(labelRepository.findExtensionNamesByLabelKeyAndLabelValue(
            eq("halo.run/hidden-user"), eq("true")))
            .thenReturn(Flux.just("user-1", "user-2"));

        var condition = new LabelEqualsCondition("halo.run/hidden-user", "true");

        resolver.resolve(List.of(condition))
            .as(StepVerifier::create)
            .assertNext(result -> {
                assertThat(result.names()).containsExactlyInAnyOrder("user-1", "user-2");
                assertThat(result.negated()).isFalse();
            })
            .verifyComplete();
    }

    @Test
    void shouldResolveLabelExistsCondition() {
        when(labelRepository.findExtensionNamesByLabelKey(eq("halo.run/hidden-user")))
            .thenReturn(Flux.just("user-1", "user-2", "user-3"));

        var condition = new LabelExistsCondition("halo.run/hidden-user");

        resolver.resolve(List.of(condition))
            .as(StepVerifier::create)
            .assertNext(result -> {
                assertThat(result.names()).containsExactlyInAnyOrder(
                    "user-1", "user-2", "user-3");
                assertThat(result.negated()).isFalse();
            })
            .verifyComplete();
    }

    @Test
    void shouldResolveLabelNotEqualsCondition() {
        when(labelRepository.findExtensionNamesByLabelKeyAndLabelValue(
            eq("halo.run/hidden-user"), eq("true")))
            .thenReturn(Flux.just("user-1"));

        var condition = new LabelNotEqualsCondition("halo.run/hidden-user", "true");

        // NotEquals returns the EXCLUDED names with negated=true
        resolver.resolve(List.of(condition))
            .as(StepVerifier::create)
            .assertNext(result -> {
                assertThat(result.names()).containsExactlyInAnyOrder("user-1");
                assertThat(result.negated()).isTrue();
            })
            .verifyComplete();
    }

    @Test
    void shouldResolveLabelInCondition() {
        when(labelRepository.findExtensionNamesByLabelKeyAndLabelValue(
            eq("category"), eq("tech")))
            .thenReturn(Flux.just("post-1", "post-2"));
        when(labelRepository.findExtensionNamesByLabelKeyAndLabelValue(
            eq("category"), eq("news")))
            .thenReturn(Flux.just("post-2", "post-3"));

        var condition = new LabelInCondition("category", List.of("tech", "news"));

        resolver.resolve(List.of(condition))
            .as(StepVerifier::create)
            .assertNext(result -> {
                assertThat(result.names()).containsExactlyInAnyOrder(
                    "post-1", "post-2", "post-3");
                assertThat(result.negated()).isFalse();
            })
            .verifyComplete();
    }

    @Test
    void shouldResolveLabelNotInCondition() {
        when(labelRepository.findExtensionNamesByLabelKeyAndLabelValue(
            eq("category"), eq("tech")))
            .thenReturn(Flux.just("post-1"));
        when(labelRepository.findExtensionNamesByLabelKeyAndLabelValue(
            eq("category"), eq("news")))
            .thenReturn(Flux.just("post-2"));

        var condition = new LabelNotInCondition("category", List.of("tech", "news"));

        // NotIn returns the EXCLUDED names with negated=true
        resolver.resolve(List.of(condition))
            .as(StepVerifier::create)
            .assertNext(result -> {
                assertThat(result.names()).containsExactlyInAnyOrder("post-1", "post-2");
                assertThat(result.negated()).isTrue();
            })
            .verifyComplete();
    }

    @Test
    void shouldIntersectMultiplePositiveConditions() {
        when(labelRepository.findExtensionNamesByLabelKeyAndLabelValue(
            eq("type"), eq("article")))
            .thenReturn(Flux.just("ext-1", "ext-2", "ext-3"));
        when(labelRepository.findExtensionNamesByLabelKey(
            eq("visible")))
            .thenReturn(Flux.just("ext-1", "ext-2", "ext-4"));

        var condition1 = new LabelEqualsCondition("type", "article");
        var condition2 = new LabelExistsCondition("visible");

        resolver.resolve(List.of(condition1, condition2))
            .as(StepVerifier::create)
            .assertNext(result -> {
                assertThat(result.names()).containsExactlyInAnyOrder("ext-1", "ext-2");
                assertThat(result.negated()).isFalse();
            })
            .verifyComplete();
    }

    @Test
    void shouldReturnEmptyNamesWhenNoLabelsMatch() {
        when(labelRepository.findExtensionNamesByLabelKeyAndLabelValue(
            anyString(), anyString()))
            .thenReturn(Flux.empty());

        var condition = new LabelEqualsCondition("nonexistent", "value");

        resolver.resolve(List.of(condition))
            .as(StepVerifier::create)
            .assertNext(result -> {
                assertThat(result.names()).isEmpty();
                assertThat(result.negated()).isFalse();
            })
            .verifyComplete();
    }

    @Test
    void shouldReturnEmptyResultForNotExistsCondition() {
        var condition = new LabelNotExistsCondition("some-key");

        resolver.resolve(List.of(condition))
            .as(StepVerifier::create)
            .assertNext(result -> {
                assertThat(result.names()).isEmpty();
                assertThat(result.negated()).isFalse();
            })
            .verifyComplete();
    }

    @Test
    void shouldReturnEmptyIntersectionWhenOneConditionMatchesNothing() {
        when(labelRepository.findExtensionNamesByLabelKeyAndLabelValue(
            eq("type"), eq("article")))
            .thenReturn(Flux.just("ext-1", "ext-2"));
        when(labelRepository.findExtensionNamesByLabelKeyAndLabelValue(
            eq("category"), eq("tech")))
            .thenReturn(Flux.empty());

        var condition1 = new LabelEqualsCondition("type", "article");
        var condition2 = new LabelEqualsCondition("category", "tech");

        resolver.resolve(List.of(condition1, condition2))
            .as(StepVerifier::create)
            .assertNext(result -> {
                assertThat(result.names()).isEmpty();
                assertThat(result.negated()).isFalse();
            })
            .verifyComplete();
    }

    @Test
    void shouldCombineTwoNegatedConditionsAsUnion() {
        // NOT label1 AND NOT label2 = NOT (label1 ∪ label2)
        // The combined result should have the union of excluded names with negated=true.
        when(labelRepository.findExtensionNamesByLabelKeyAndLabelValue(
            eq("region"), eq("us-east")))
            .thenReturn(Flux.just("ext-1", "ext-2"));
        when(labelRepository.findExtensionNamesByLabelKeyAndLabelValue(
            eq("tier"), eq("free")))
            .thenReturn(Flux.just("ext-2", "ext-3"));

        var condition1 = new LabelNotEqualsCondition("region", "us-east");
        var condition2 = new LabelNotEqualsCondition("tier", "free");

        resolver.resolve(List.of(condition1, condition2))
            .as(StepVerifier::create)
            .assertNext(result -> {
                // Should be the union of {ext-1, ext-2} and {ext-2, ext-3}
                assertThat(result.names()).containsExactlyInAnyOrder("ext-1", "ext-2", "ext-3");
                assertThat(result.negated()).isTrue();
            })
            .verifyComplete();
    }
}
