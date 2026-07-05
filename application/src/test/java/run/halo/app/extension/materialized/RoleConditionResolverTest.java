package run.halo.app.extension.materialized;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
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
import run.halo.app.extension.index.query.InCondition;
import run.halo.app.extension.index.query.IndexCondition;

/**
 * Tests for {@link RoleConditionResolver}.
 *
 * @author halo
 * @since 2.22.0
 */
@ExtendWith(MockitoExtension.class)
class RoleConditionResolverTest {

    @Mock
    UserRoleRepository roleRepository;

    RoleConditionResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new RoleConditionResolver(roleRepository);
    }

    @Test
    void shouldReturnEmptySetForEmptyConditions() {
        resolver.resolve(List.of())
            .as(StepVerifier::create)
            .expectNext(Set.of())
            .verifyComplete();
    }

    @Test
    void shouldResolveInConditionForSingleRole() {
        when(roleRepository.findStoreNamesByRoleNameIn(Set.of("admin")))
            .thenReturn(Flux.just("user-1", "user-2"));

        var condition = new InCondition("roles", List.of("admin"));

        resolver.resolve(List.of(condition))
            .as(StepVerifier::create)
            .expectNext(Set.of("user-1", "user-2"))
            .verifyComplete();
    }

    @Test
    void shouldResolveInConditionForMultipleRoles() {
        when(roleRepository.findStoreNamesByRoleNameIn(Set.of("admin", "editor")))
            .thenReturn(Flux.just("user-1", "user-2", "user-3"));

        var condition = new InCondition("roles", List.of("admin", "editor"));

        resolver.resolve(List.of(condition))
            .as(StepVerifier::create)
            .assertNext(result -> assertThat(result).containsExactlyInAnyOrder(
                "user-1", "user-2", "user-3"))
            .verifyComplete();
    }

    @Test
    void shouldReturnEmptySetWhenNoRolesMatch() {
        when(roleRepository.findStoreNamesByRoleNameIn(anyCollection()))
            .thenReturn(Flux.empty());

        var condition = new InCondition("roles", List.of("nonexistent-role"));

        resolver.resolve(List.of(condition))
            .as(StepVerifier::create)
            .expectNext(Set.of())
            .verifyComplete();
    }

    @Test
    void shouldThrowExceptionForUnknownConditionType() {
        var unknownCondition = new IndexCondition() {
            @Override
            public String indexName() {
                return "roles";
            }
        };

        assertThatThrownBy(() -> resolver.resolve(List.of(unknownCondition)).block())
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessageContaining("Unknown role condition type");
    }
}
