package run.halo.app.extension.materialized;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Sort;
import reactor.test.StepVerifier;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.ListResult;
import run.halo.app.extension.PageRequestImpl;
import run.halo.app.extension.index.query.EqualCondition;
import run.halo.app.extension.index.query.InCondition;
import run.halo.app.extension.index.query.IsNullCondition;
import run.halo.app.extension.index.query.LabelEqualsCondition;
import run.halo.app.extension.router.selector.FieldSelector;
import run.halo.app.extension.router.selector.LabelSelector;

/**
 * Integration tests for {@link UserSqlQueryEngine} against H2.
 *
 * @author johnniang
 * @since 2.22.0
 */
@SpringBootTest
class UserSqlQueryEngineTest {

    @Autowired
    UserSqlQueryEngine engine;

    @Autowired
    UserRepository userRepository;

    @Autowired
    ExtensionLabelRepository extensionLabelRepository;

    @Autowired
    UserRoleRepository userRoleRepository;

    @BeforeEach
    void cleanUp() {
        userRepository.deleteAll().block();
        extensionLabelRepository.deleteAll().block();
        userRoleRepository.deleteAll().block();
    }

    @Test
    void listAllWithEmptyOptionsShouldReturnAllUsers() {
        seedUsers();

        engine.listAll(new ListOptions(), Sort.by(Sort.Direction.ASC, "name"))
            .as(StepVerifier::create)
            .expectNextCount(3)
            .verifyComplete();
    }

    @Test
    void listByEqualEmailShouldReturnMatchingUser() {
        seedUsers();

        var options = new ListOptions();
        options.setFieldSelector(FieldSelector.of(
            new EqualCondition("spec.email", "a@b.com")));

        engine.listBy(options, Sort.by(Sort.Direction.ASC, "name"),
                PageRequestImpl.of(1, 10))
            .as(StepVerifier::create)
            .assertNext(result -> {
                assertThat(result.getTotal()).isEqualTo(1);
                assertThat(result.getItems()).hasSize(1);
                assertThat(result.getItems().get(0).getMetadata().getName())
                    .isEqualTo("alice");
                assertThat(result.getItems().get(0).getSpec().getEmail())
                    .isEqualTo("a@b.com");
            })
            .verifyComplete();
    }

    @Test
    void listByDisabledFalseShouldReturnEnabledUsers() {
        seedUsers();

        var options = new ListOptions();
        options.setFieldSelector(FieldSelector.of(
            new EqualCondition("spec.disabled", false)));

        engine.listBy(options, Sort.by(Sort.Direction.ASC, "name"),
                PageRequestImpl.of(1, 10))
            .as(StepVerifier::create)
            .assertNext(result -> {
                assertThat(result.getTotal()).isEqualTo(2);
                assertThat(result.getItems()).hasSize(2);
                var names = result.getItems().stream()
                    .map(u -> u.getMetadata().getName())
                    .toList();
                assertThat(names).containsExactly("alice", "bob");
            })
            .verifyComplete();
    }

    @Test
    void listByNullDeletionTimestampShouldReturnNonDeletedUsers() {
        userRepository.save(createUser("alice", "Alice", "a@b.com")).block();
        userRepository.save(createUser("bob", "Bob", "b@c.com")).block();

        var deleted = createUser("deleted", "Deleted", "d@e.com");
        deleted.setDeletionTimestamp(Instant.now());
        userRepository.save(deleted).block();

        var options = new ListOptions();
        options.setFieldSelector(FieldSelector.of(
            new IsNullCondition("metadata.deletionTimestamp")));

        engine.listBy(options, Sort.by(Sort.Direction.ASC, "name"),
                PageRequestImpl.of(1, 10))
            .as(StepVerifier::create)
            .assertNext(result -> {
                assertThat(result.getTotal()).isEqualTo(2);
                var names = result.getItems().stream()
                    .map(u -> u.getMetadata().getName())
                    .toList();
                assertThat(names).containsExactly("alice", "bob");
            })
            .verifyComplete();
    }

    @Test
    void listByAndOfMultipleConditionsShouldReturnIntersection() {
        seedUsers();

        var options = new ListOptions();
        options.setFieldSelector(FieldSelector.of(
            new EqualCondition("spec.disabled", false)
                .and(new IsNullCondition("metadata.deletionTimestamp"))));

        engine.listBy(options, Sort.by(Sort.Direction.ASC, "name"),
                PageRequestImpl.of(1, 10))
            .as(StepVerifier::create)
            .assertNext(result -> {
                assertThat(result.getTotal()).isEqualTo(2);
                var names = result.getItems().stream()
                    .map(u -> u.getMetadata().getName())
                    .toList();
                assertThat(names).containsExactly("alice", "bob");
            })
            .verifyComplete();
    }

    @Test
    void listByLabelEqualsConditionShouldResolveLabel() {
        userRepository.save(createUser("alice", "Alice", "a@b.com")).block();
        userRepository.save(createUser("bob", "Bob", "b@c.com")).block();

        extensionLabelRepository.insert(
            createLabel("alice", "team", "backend")).block();
        extensionLabelRepository.insert(
            createLabel("bob", "team", "frontend")).block();

        var options = new ListOptions();
        options.setLabelSelector(
            new LabelSelector().setConditions(
                List.of(new LabelEqualsCondition("team", "backend"))));

        engine.listBy(options, Sort.by(Sort.Direction.ASC, "name"),
                PageRequestImpl.of(1, 10))
            .as(StepVerifier::create)
            .assertNext(result -> {
                assertThat(result.getTotal()).isEqualTo(1);
                assertThat(result.getItems().get(0).getMetadata().getName())
                    .isEqualTo("alice");
            })
            .verifyComplete();
    }

    @Test
    void listByRoleConditionShouldResolveRoles() {
        userRepository.save(createUser("alice", "Alice", "a@b.com")).block();
        userRepository.save(createUser("bob", "Bob", "b@c.com")).block();

        userRoleRepository.insert(createRole("alice", "admin")).block();
        userRoleRepository.insert(createRole("alice", "editor")).block();
        userRoleRepository.insert(createRole("bob", "editor")).block();

        var options = new ListOptions();
        options.setFieldSelector(FieldSelector.of(
            new InCondition("roles", List.of("admin"))));

        engine.listBy(options, Sort.by(Sort.Direction.ASC, "name"),
                PageRequestImpl.of(1, 10))
            .as(StepVerifier::create)
            .assertNext(result -> {
                assertThat(result.getTotal()).isEqualTo(1);
                assertThat(result.getItems().get(0).getMetadata().getName())
                    .isEqualTo("alice");
            })
            .verifyComplete();
    }

    @Test
    void listByCombinedFieldAndLabelShouldReturnIntersection() {
        userRepository.save(createUser("alice", "Alice", "a@b.com")).block();
        userRepository.save(createUser("bob", "Bob", "b@c.com")).block();

        extensionLabelRepository.insert(
            createLabel("alice", "team", "backend")).block();
        extensionLabelRepository.insert(
            createLabel("bob", "team", "backend")).block();

        var options = new ListOptions();
        options.setLabelSelector(
            new LabelSelector().setConditions(
                List.of(new LabelEqualsCondition("team", "backend"))));
        options.setFieldSelector(FieldSelector.of(
            new EqualCondition("spec.email", "a@b.com")));

        engine.listBy(options, Sort.by(Sort.Direction.ASC, "name"),
                PageRequestImpl.of(1, 10))
            .as(StepVerifier::create)
            .assertNext(result -> {
                assertThat(result.getTotal()).isEqualTo(1);
                assertThat(result.getItems().get(0).getMetadata().getName())
                    .isEqualTo("alice");
            })
            .verifyComplete();
    }

    @Test
    void countByWithConditionsShouldReturnCorrectCount() {
        seedUsers();

        var options = new ListOptions();
        options.setFieldSelector(FieldSelector.of(
            new EqualCondition("spec.disabled", false)));

        engine.countBy(options)
            .as(StepVerifier::create)
            .expectNext(2L)
            .verifyComplete();
    }

    @Test
    void listAllNamesShouldReturnOnlyNames() {
        seedUsers();

        var options = new ListOptions();
        options.setFieldSelector(FieldSelector.of(
            new EqualCondition("spec.disabled", false)));

        engine.listAllNames(options, Sort.by(Sort.Direction.ASC, "name"))
            .collectList()
            .as(StepVerifier::create)
            .assertNext(names -> assertThat(names).containsExactly("alice", "bob"))
            .verifyComplete();
    }

    @Test
    void listByWithSortAndPaginationShouldReturnCorrectPage() {
        userRepository.save(createUser("alice", "Alice", "a@b.com")).block();
        userRepository.save(createUser("bob", "Bob", "b@c.com")).block();
        userRepository.save(createUser("charlie", "Charlie", "c@d.com")).block();

        engine.listBy(new ListOptions(), Sort.by(Sort.Direction.ASC, "name"),
                PageRequestImpl.of(2, 2))
            .as(StepVerifier::create)
            .assertNext(result -> {
                assertThat(result.getTotal()).isEqualTo(3);
                assertThat(result.getPage()).isEqualTo(2);
                assertThat(result.getSize()).isEqualTo(2);
                assertThat(result.getItems()).hasSize(1);
                assertThat(result.getItems().get(0).getMetadata().getName())
                    .isEqualTo("charlie");
            })
            .verifyComplete();
    }

    @Test
    void listByWithNoMatchingResultsShouldReturnEmptyList() {
        userRepository.save(createUser("alice", "Alice", "a@b.com")).block();

        var options = new ListOptions();
        options.setFieldSelector(FieldSelector.of(
            new EqualCondition("spec.email", "nonexistent@example.com")));

        engine.listBy(options, Sort.by(Sort.Direction.ASC, "name"),
                PageRequestImpl.of(1, 10))
            .as(StepVerifier::create)
            .assertNext(result -> {
                assertThat(result.getTotal()).isEqualTo(0);
                assertThat(result.getItems()).isEmpty();
            })
            .verifyComplete();
    }

    @Test
    void listAllShouldReturnAllMatchingUsers() {
        seedUsers();

        var options = new ListOptions();
        options.setFieldSelector(FieldSelector.of(
            new EqualCondition("spec.disabled", false)));

        engine.listAll(options, Sort.by(Sort.Direction.ASC, "name"))
            .collectList()
            .as(StepVerifier::create)
            .assertNext(users -> assertThat(users).hasSize(2))
            .verifyComplete();
    }

    // ---- Helpers ----

    private void seedUsers() {
        userRepository.save(createUser("alice", "Alice", "a@b.com")).block();
        userRepository.save(createUser("bob", "Bob", "b@c.com")).block();

        var charlie = createUser("charlie", "Charlie", "c@d.com");
        charlie.setDisabled(true);
        userRepository.save(charlie).block();
    }

    private UserPo createUser(String name, String displayName, String email) {
        var user = new UserPo();
        user.setName(name);
        user.setDisplayName(displayName);
        user.setEmail(email);
        user.setCreationTimestamp(Instant.now());
        return user;
    }

    private ExtensionLabel createLabel(String extensionName, String key, String value) {
        var label = new ExtensionLabel();
        var id = new ExtensionLabelId();
        id.setExtensionName(extensionName);
        id.setLabelKey(key);
        label.setId(id);
        label.setLabelValue(value);
        return label;
    }

    private UserRole createRole(String storeName, String roleName) {
        var role = new UserRole();
        var id = new UserRoleId();
        id.setStoreName(storeName);
        id.setRoleName(roleName);
        role.setId(id);
        return role;
    }
}
