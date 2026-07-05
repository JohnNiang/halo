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
import reactor.core.publisher.Mono;
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
    void shouldReturnEmptySetForEmptyConditions() {
        resolver.resolve(List.of())
            .as(StepVerifier::create)
            .expectNext(Set.of())
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
            .expectNext(Set.of("user-1", "user-2"))
            .verifyComplete();
    }

    @Test
    void shouldResolveLabelExistsCondition() {
        when(labelRepository.findExtensionNamesByLabelKey(eq("halo.run/hidden-user")))
            .thenReturn(Flux.just("user-1", "user-2", "user-3"));

        var condition = new LabelExistsCondition("halo.run/hidden-user");

        resolver.resolve(List.of(condition))
            .as(StepVerifier::create)
            .expectNext(Set.of("user-1", "user-2", "user-3"))
            .verifyComplete();
    }

    @Test
    void shouldResolveLabelNotEqualsCondition() {
        when(labelRepository.findExtensionNamesByLabelKey(eq("halo.run/hidden-user")))
            .thenReturn(Flux.just("user-1", "user-2", "user-3"));
        when(labelRepository.findExtensionNamesByLabelKeyAndLabelValue(
            eq("halo.run/hidden-user"), eq("true")))
            .thenReturn(Flux.just("user-1"));

        var condition = new LabelNotEqualsCondition("halo.run/hidden-user", "true");

        resolver.resolve(List.of(condition))
            .as(StepVerifier::create)
            .expectNext(Set.of("user-2", "user-3"))
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
            .assertNext(result -> assertThat(result).containsExactlyInAnyOrder(
                "post-1", "post-2", "post-3"))
            .verifyComplete();
    }

    @Test
    void shouldResolveLabelNotInCondition() {
        when(labelRepository.findExtensionNamesByLabelKey(eq("category")))
            .thenReturn(Flux.just("post-1", "post-2", "post-3", "post-4"));
        when(labelRepository.findExtensionNamesByLabelKeyAndLabelValue(
            eq("category"), eq("tech")))
            .thenReturn(Flux.just("post-1"));
        when(labelRepository.findExtensionNamesByLabelKeyAndLabelValue(
            eq("category"), eq("news")))
            .thenReturn(Flux.just("post-2"));

        var condition = new LabelNotInCondition("category", List.of("tech", "news"));

        resolver.resolve(List.of(condition))
            .as(StepVerifier::create)
            .assertNext(result -> assertThat(result).containsExactlyInAnyOrder(
                "post-3", "post-4"))
            .verifyComplete();
    }

    @Test
    void shouldIntersectMultipleConditions() {
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
            .assertNext(result -> assertThat(result).containsExactlyInAnyOrder(
                "ext-1", "ext-2"))
            .verifyComplete();
    }

    @Test
    void shouldReturnEmptySetWhenNoLabelsMatch() {
        when(labelRepository.findExtensionNamesByLabelKeyAndLabelValue(
            anyString(), anyString()))
            .thenReturn(Flux.empty());

        var condition = new LabelEqualsCondition("nonexistent", "value");

        resolver.resolve(List.of(condition))
            .as(StepVerifier::create)
            .expectNext(Set.of())
            .verifyComplete();
    }

    @Test
    void shouldReturnEmptySetForNotExistsCondition() {
        var condition = new LabelNotExistsCondition("some-key");

        resolver.resolve(List.of(condition))
            .as(StepVerifier::create)
            .expectNext(Set.of())
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
            .expectNext(Set.of())
            .verifyComplete();
    }
}
