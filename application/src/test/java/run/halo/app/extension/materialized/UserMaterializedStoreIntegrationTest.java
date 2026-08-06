package run.halo.app.extension.materialized;

import static org.assertj.core.api.Assertions.assertThat;
import static run.halo.app.extension.index.query.Queries.equal;
import static run.halo.app.extension.index.query.Queries.in;
import static run.halo.app.extension.index.query.Queries.isNull;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Sort;
import org.springframework.test.annotation.DirtiesContext;
import reactor.test.StepVerifier;
import run.halo.app.core.extension.User;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.Metadata;
import run.halo.app.extension.PageRequestImpl;
import run.halo.app.extension.ReactiveExtensionClient;

/**
 * End-to-end integration tests for the User materialized store feature.
 *
 * <p>Verifies that CRUD operations through {@link ReactiveExtensionClient} correctly sync
 * to the materialized tables ({@code users}, {@code extension_labels}, {@code user_roles}),
 * and that query operations ({@code listBy}, {@code listAll}, {@code countBy}) use the
 * SQL-based {@link UserSqlQueryEngine} path.
 *
 * @author halo
 * @since 2.22.0
 */
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class UserMaterializedStoreIntegrationTest {

    @Autowired
    ReactiveExtensionClient client;

    @Autowired
    UserRepository userRepository;

    @Autowired
    ExtensionLabelRepository labelRepository;

    @Autowired
    UserRoleRepository roleRepository;

    @BeforeEach
    void cleanUp() {
        userRepository.deleteAll().block();
        labelRepository.deleteAll().block();
        roleRepository.deleteAll().block();
    }

    // ======================================================================
    // CRUD Tests — create / update / delete via ReactiveExtensionClient
    // ======================================================================

    @Nested
    class CrudTests {

        @Test
        void createUserShouldSyncToMaterializedTable() {
            var user = createUser("testuser", "Test User", "test@example.com");

            client.create(user)
                .then(userRepository.findById("testuser"))
                .as(StepVerifier::create)
                .assertNext(found -> {
                    assertThat(found.getName()).isEqualTo("testuser");
                    assertThat(found.getDisplayName()).isEqualTo("Test User");
                    assertThat(found.getEmail()).isEqualTo("test@example.com");
                    assertThat(found.isEmailVerified()).isFalse();
                    assertThat(found.isDisabled()).isFalse();
                    assertThat(found.getCreationTimestamp()).isNotNull();
                })
                .verifyComplete();
        }

        @Test
        void updateUserShouldSyncChangesToMaterializedTable() {
            var user = createUser("testuser", "Original Name", "test@example.com");

            client.create(user)
                .flatMap(created -> {
                    created.getSpec().setDisplayName("Updated Name");
                    return client.update(created);
                })
                .then(userRepository.findById("testuser"))
                .as(StepVerifier::create)
                .assertNext(found -> assertThat(found.getDisplayName()).isEqualTo("Updated Name"))
                .verifyComplete();
        }

        @Test
        void deleteUserShouldSetDeletionTimestampInMaterializedTable() {
            var user = createUser("testuser", "Test User", "test@example.com");

            client.create(user)
                .flatMap(created -> client.delete(created))
                .then(userRepository.findById("testuser"))
                .as(StepVerifier::create)
                .assertNext(found -> assertThat(found.getDeletionTimestamp()).isNotNull())
                .verifyComplete();
        }
    }

    // ======================================================================
    // Query Tests — field conditions, label conditions, pagination, sorting
    // ======================================================================

    @Nested
    class QueryTests {

        @Test
        void queryByEmailShouldReturnMatchingUser() {
            seedThreeUsers();

            var options = ListOptions.builder()
                .andQuery(equal("spec.email", "a@test.com"))
                .build();

            client.listBy(User.class, options, PageRequestImpl.of(1, 10))
                .as(StepVerifier::create)
                .assertNext(result -> {
                    assertThat(result.getTotal()).isEqualTo(1);
                    assertThat(result.getItems()).hasSize(1);
                    assertThat(result.getItems().get(0).getMetadata().getName())
                        .isEqualTo("alice");
                })
                .verifyComplete();
        }

        @Test
        void queryByDisabledStatusShouldReturnOnlyEnabledUsers() {
            var alice = createUser("alice", "Alice", "a@test.com");
            var bob = createUser("bob", "Bob", "b@test.com");
            bob.getSpec().setDisabled(true);

            client.create(alice)
                .then(client.create(bob))
                .then()
                .block();

            var options = ListOptions.builder()
                .andQuery(equal("spec.disabled", false))
                .build();

            client.listBy(User.class, options, PageRequestImpl.of(1, 10))
                .as(StepVerifier::create)
                .assertNext(result -> {
                    assertThat(result.getTotal()).isEqualTo(1);
                    assertThat(result.getItems().get(0).getMetadata().getName())
                        .isEqualTo("alice");
                })
                .verifyComplete();
        }

        @Test
        void queryByEmailVerifiedShouldReturnCorrectResults() {
            var alice = createUser("alice", "Alice", "a@test.com");
            alice.getSpec().setEmailVerified(true);
            var bob = createUser("bob", "Bob", "b@test.com");
            bob.getSpec().setEmailVerified(false);

            client.create(alice)
                .then(client.create(bob))
                .then()
                .block();

            var options = ListOptions.builder()
                .andQuery(equal("spec.emailVerified", true))
                .build();

            client.listBy(User.class, options, PageRequestImpl.of(1, 10))
                .as(StepVerifier::create)
                .assertNext(result -> {
                    assertThat(result.getTotal()).isEqualTo(1);
                    assertThat(result.getItems().get(0).getMetadata().getName())
                        .isEqualTo("alice");
                })
                .verifyComplete();
        }

        @Test
        void queryWithAndMultipleConditionsShouldReturnIntersection() {
            var alice = createUser("alice", "Alice", "a@test.com");
            alice.getSpec().setDisabled(false);
            alice.getSpec().setEmailVerified(true);
            var bob = createUser("bob", "Bob", "b@test.com");
            bob.getSpec().setDisabled(false);
            bob.getSpec().setEmailVerified(false);
            var charlie = createUser("charlie", "Charlie", "c@test.com");
            charlie.getSpec().setDisabled(true);
            charlie.getSpec().setEmailVerified(true);

            client.create(alice)
                .then(client.create(bob))
                .then(client.create(charlie))
                .then()
                .block();

            var options = ListOptions.builder()
                .andQuery(equal("spec.disabled", false))
                .andQuery(equal("spec.emailVerified", true))
                .build();

            client.listBy(User.class, options, PageRequestImpl.of(1, 10))
                .as(StepVerifier::create)
                .assertNext(result -> {
                    assertThat(result.getTotal()).isEqualTo(1);
                    assertThat(result.getItems().get(0).getMetadata().getName())
                        .isEqualTo("alice");
                })
                .verifyComplete();
        }

        @Test
        void queryWithLabelConditionShouldExcludeMatchingUsers() {
            var alice = createUser("alice", "Alice", "a@test.com");
            var bob = createUser("bob", "Bob", "b@test.com");
            bob.getMetadata().setLabels(Map.of("halo.run/hidden-user", "true"));

            client.create(alice)
                .then(client.create(bob))
                .then()
                .block();

            var options = ListOptions.builder()
                .labelSelector()
                    .notEq("halo.run/hidden-user", "true")
                .end()
                .build();

            client.listBy(User.class, options, PageRequestImpl.of(1, 10))
                .as(StepVerifier::create)
                .assertNext(result -> {
                    assertThat(result.getTotal()).isEqualTo(1);
                    assertThat(result.getItems().get(0).getMetadata().getName())
                        .isEqualTo("alice");
                })
                .verifyComplete();
        }

        @Test
        void queryWithPaginationShouldReturnCorrectPage() {
            for (int i = 1; i <= 10; i++) {
                client.create(createUser("user" + i, "User " + i, "user"
                    + i + "@test.com")).block();
            }

            var page1 = client.listBy(User.class, new ListOptions(),
                    PageRequestImpl.of(1, 3, Sort.by(Sort.Direction.ASC, "name")))
                .block();
            var page2 = client.listBy(User.class, new ListOptions(),
                    PageRequestImpl.of(2, 3, Sort.by(Sort.Direction.ASC, "name")))
                .block();

            assertThat(page1).isNotNull();
            assertThat(page1.getTotal()).isEqualTo(10);
            assertThat(page1.getItems()).hasSize(3);
            assertThat(page1.getPage()).isEqualTo(1);

            assertThat(page2).isNotNull();
            assertThat(page2.getTotal()).isEqualTo(10);
            assertThat(page2.getItems()).hasSize(3);
            assertThat(page2.getPage()).isEqualTo(2);

            // Ensure no overlap between pages
            var page1Names = page1.getItems().stream()
                .map(u -> u.getMetadata().getName()).toList();
            var page2Names = page2.getItems().stream()
                .map(u -> u.getMetadata().getName()).toList();
            assertThat(page1Names).doesNotContainAnyElementsOf(page2Names);
        }

        @Test
        void queryWithSortingShouldReturnResultsInOrder() {
            // Create users with deliberate ordering by inserting with a small delay
            var alice = createUser("alice", "Alice", "a@test.com");
            client.create(alice).block();
            var bob = createUser("bob", "Bob", "b@test.com");
            client.create(bob).block();
            var charlie = createUser("charlie", "Charlie", "c@test.com");
            client.create(charlie).block();

            var options = new ListOptions();
            var result = client.listBy(User.class, options,
                    PageRequestImpl.of(1, 10, Sort.by(Sort.Direction.DESC, "creationTimestamp")))
                .block();

            assertThat(result).isNotNull();
            assertThat(result.getItems()).hasSize(3);
            // Most recently created should be first (descending order)
            assertThat(result.getItems().get(0).getMetadata().getName())
                .isEqualTo("charlie");
            assertThat(result.getItems().get(2).getMetadata().getName())
                .isEqualTo("alice");
        }

        @Test
        void countByShouldReturnCorrectCount() {
            for (int i = 1; i <= 5; i++) {
                client.create(createUser("user" + i, "User " + i, "user"
                    + i + "@test.com")).block();
            }

            // Count all (no deletion timestamp)
            var options = ListOptions.builder()
                .andQuery(isNull("metadata.deletionTimestamp"))
                .build();

            client.countBy(User.class, options)
                .as(StepVerifier::create)
                .expectNext(5L)
                .verifyComplete();
        }

        @Test
        void countByAfterDeleteShouldExcludeDeletedUsers() {
            var alice = createUser("alice", "Alice", "a@test.com");
            var bob = createUser("bob", "Bob", "b@test.com");

            client.create(alice)
                .then(client.create(bob))
                .flatMap(created -> client.delete(created))
                .then()
                .block();

            var options = ListOptions.builder()
                .andQuery(isNull("metadata.deletionTimestamp"))
                .build();

            client.countBy(User.class, options)
                .as(StepVerifier::create)
                .expectNext(1L)
                .verifyComplete();
        }

        @Test
        void queryByNameInShouldReturnOnlyMatchingUsers() {
            for (int i = 1; i <= 5; i++) {
                client.create(createUser("user" + i, "User " + i, "user"
                    + i + "@test.com")).block();
            }

            @SuppressWarnings("unchecked")
            var options = ListOptions.builder()
                .andQuery(in("metadata.name",
                    (Object) List.of("user1", "user3")))
                .build();

            client.listAll(User.class, options, Sort.by(Sort.Direction.ASC, "name"))
                .collectList()
                .as(StepVerifier::create)
                .assertNext(users -> {
                    assertThat(users).hasSize(2);
                    var names = users.stream()
                        .map(u -> u.getMetadata().getName()).toList();
                    assertThat(names).containsExactly("user1", "user3");
                })
                .verifyComplete();
        }
    }

    // ======================================================================
    // Edge Cases
    // ======================================================================

    @Nested
    class EdgeCaseTests {

        @Test
        void queryWithNoResultsShouldReturnEmptyList() {
            var user = createUser("alice", "Alice", "a@test.com");
            client.create(user).block();

            var options = ListOptions.builder()
                .andQuery(equal("spec.email", "nonexistent@example.com"))
                .build();

            client.listBy(User.class, options, PageRequestImpl.of(1, 10))
                .as(StepVerifier::create)
                .assertNext(result -> {
                    assertThat(result.getTotal()).isEqualTo(0);
                    assertThat(result.getItems()).isEmpty();
                })
                .verifyComplete();
        }

        @Test
        void userWithNoLabelsShouldHandleLabelQueryGracefully() {
            var alice = createUser("alice", "Alice", "a@test.com");
            // No labels set
            client.create(alice).block();

            var bob = createUser("bob", "Bob", "b@test.com");
            bob.getMetadata().setLabels(Map.of("team", "backend"));
            client.create(bob).block();

            var options = ListOptions.builder()
                .labelSelector()
                    .eq("team", "backend")
                .end()
                .build();

            client.listBy(User.class, options, PageRequestImpl.of(1, 10))
                .as(StepVerifier::create)
                .assertNext(result -> {
                    assertThat(result.getTotal()).isEqualTo(1);
                    assertThat(result.getItems().get(0).getMetadata().getName())
                        .isEqualTo("bob");
                })
                .verifyComplete();
        }

        @Test
        void userWithNoRolesShouldHandleRoleQueryGracefully() {
            var alice = createUser("alice", "Alice", "a@test.com");
            // No role annotations
            client.create(alice).block();

            var bob = createUser("bob", "Bob", "b@test.com");
            bob.getMetadata().setAnnotations(Map.of(
                User.ROLE_NAMES_ANNO, "[\"admin\",\"editor\"]"
            ));
            client.create(bob).block();

            // Use 2 values so Queries.in() produces InCondition (not EqualCondition)
            @SuppressWarnings("unchecked")
            var options = ListOptions.builder()
                .andQuery(in("roles", (Object) List.of("admin", "viewer")))
                .build();

            client.listBy(User.class, options, PageRequestImpl.of(1, 10))
                .as(StepVerifier::create)
                .assertNext(result -> {
                    assertThat(result.getTotal()).isEqualTo(1);
                    assertThat(result.getItems().get(0).getMetadata().getName())
                        .isEqualTo("bob");
                })
                .verifyComplete();
        }
    }

    // ======================================================================
    // Helpers
    // ======================================================================

    private User createUser(String name, String displayName, String email) {
        var metadata = new Metadata();
        metadata.setName(name);
        var spec = new User.UserSpec();
        spec.setDisplayName(displayName);
        spec.setEmail(email);
        var user = new User();
        user.setMetadata(metadata);
        user.setSpec(spec);
        return user;
    }

    private void seedThreeUsers() {
        var alice = createUser("alice", "Alice", "a@test.com");
        var bob = createUser("bob", "Bob", "b@test.com");
        var charlie = createUser("charlie", "Charlie", "c@test.com");
        client.create(alice).block();
        client.create(bob).block();
        client.create(charlie).block();
    }
}
