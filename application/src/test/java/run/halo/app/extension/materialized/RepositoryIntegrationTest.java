package run.halo.app.extension.materialized;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Sort;
import reactor.test.StepVerifier;

/**
 * Integration tests for the materialized R2DBC repositories against H2.
 *
 * @author halo
 * @since 2.21.0
 */
@SpringBootTest
class RepositoryIntegrationTest {

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

    // ---- UserRepository tests ----

    @Test
    void shouldSaveAndFindUser() {
        var user = createUser("admin", "Admin User", "admin@example.com");

        userRepository.save(user)
            .then(userRepository.findById("admin"))
            .as(StepVerifier::create)
            .expectNextMatches(found -> found.getName().equals("admin")
                && found.getDisplayName().equals("Admin User")
                && found.getEmail().equals("admin@example.com")
                && !found.isEmailVerified()
                && !found.isDisabled())
            .verifyComplete();
    }

    @Test
    void shouldFindByEmail() {
        var user = createUser("admin", "Admin User", "admin@example.com");

        userRepository.save(user)
            .then(userRepository.findByEmail("admin@example.com"))
            .as(StepVerifier::create)
            .expectNextMatches(found -> found.getName().equals("admin"))
            .verifyComplete();
    }

    @Test
    void shouldReturnEmptyForNonexistentEmail() {
        userRepository.findByEmail("nobody@example.com")
            .as(StepVerifier::create)
            .verifyComplete();
    }

    @Test
    void shouldFindAllByEmailVerified() {
        var verified = createUser("verified", "Verified", "verified@example.com");
        verified.setEmailVerified(true);
        var unverified = createUser("unverified", "Unverified", "unverified@example.com");
        unverified.setEmailVerified(false);

        userRepository.save(verified)
            .then(userRepository.save(unverified))
            .thenMany(userRepository.findAllByEmailVerified(true))
            .as(StepVerifier::create)
            .expectNextMatches(found -> found.getName().equals("verified"))
            .verifyComplete();
    }

    @Test
    void shouldFindAllByDisabled() {
        var enabled = createUser("enabled", "Enabled", "enabled@example.com");
        enabled.setDisabled(false);
        var disabled = createUser("disabled", "Disabled", "disabled@example.com");
        disabled.setDisabled(true);

        userRepository.save(enabled)
            .then(userRepository.save(disabled))
            .thenMany(userRepository.findAllByDisabled(true))
            .as(StepVerifier::create)
            .expectNextMatches(found -> found.getName().equals("disabled"))
            .verifyComplete();
    }

    @Test
    void shouldFindAllByNameIn() {
        userRepository.save(createUser("alice", "Alice", "a@e.com"))
            .then(userRepository.save(createUser("bob", "Bob", "b@e.com")))
            .then(userRepository.save(createUser("charlie", "Charlie", "c@e.com")))
            .thenMany(userRepository.findAllByNameIn(List.of("alice", "charlie")))
            .as(StepVerifier::create)
            .expectNextCount(2)
            .verifyComplete();
    }

    @Test
    void shouldFindAllWithSort() {
        userRepository.save(createUser("bob", "Bob", "b@e.com"))
            .then(userRepository.save(createUser("alice", "Alice", "a@e.com")))
            .thenMany(userRepository.findAll(Sort.by(Sort.Direction.ASC, "name")))
            .as(StepVerifier::create)
            .expectNextMatches(u -> u.getName().equals("alice"))
            .expectNextMatches(u -> u.getName().equals("bob"))
            .verifyComplete();
    }

    @Test
    void shouldOptimisticLockVersion() {
        var user = createUser("admin", "Admin", "admin@example.com");

        userRepository.save(user)
            .flatMap(saved -> {
                saved.setDisplayName("Updated Admin");
                return userRepository.save(saved);
            })
            .as(StepVerifier::create)
            .expectNextMatches(updated -> updated.getVersion() != null
                && updated.getDisplayName().equals("Updated Admin"))
            .verifyComplete();
    }

    @Test
    void shouldHandleTimestampFields() {
        var now = Instant.now();
        var user = createUser("admin", "Admin", "admin@example.com");
        user.setCreationTimestamp(now);
        user.setDeletionTimestamp(now.plusSeconds(3600));

        userRepository.save(user)
            .then(userRepository.findById("admin"))
            .as(StepVerifier::create)
            .expectNextMatches(found -> found.getCreationTimestamp() != null
                && found.getDeletionTimestamp() != null)
            .verifyComplete();
    }

    // ---- ExtensionLabelRepository tests ----

    @Test
    void shouldInsertAndFindExtensionLabel() {
        var label = createLabel("/registry/v1alpha1/users/admin", "role", "admin");

        extensionLabelRepository.insert(label)
            .as(StepVerifier::create)
            .expectNextMatches(saved -> saved.getId().getLabelKey().equals("role"))
            .verifyComplete();
    }

    @Test
    void shouldFindExtensionNamesByLabelKeyAndLabelValue() {
        extensionLabelRepository.insert(
                createLabel("/registry/v1alpha1/users/admin", "role", "admin"))
            .then(extensionLabelRepository.insert(
                createLabel("/registry/v1alpha1/users/editor", "role", "editor")))
            .then(extensionLabelRepository.insert(
                createLabel("/registry/v1alpha1/users/super", "role", "admin")))
            .thenMany(extensionLabelRepository.findExtensionNamesByLabelKeyAndLabelValue(
                "role", "admin"))
            .as(StepVerifier::create)
            .expectNextCount(2)
            .verifyComplete();
    }

    @Test
    void shouldFindExtensionNamesByLabelKey() {
        extensionLabelRepository.insert(
                createLabel("/registry/v1alpha1/users/admin", "role", "admin"))
            .then(extensionLabelRepository.insert(
                createLabel("/registry/v1alpha1/users/admin", "tier", "premium")))
            .thenMany(extensionLabelRepository.findExtensionNamesByLabelKey("tier"))
            .as(StepVerifier::create)
            .expectNext("/registry/v1alpha1/users/admin")
            .verifyComplete();
    }

    @Test
    void shouldDeleteByExtensionName() {
        extensionLabelRepository.insert(
                createLabel("/registry/v1alpha1/users/admin", "role", "admin"))
            .then(extensionLabelRepository.insert(
                createLabel("/registry/v1alpha1/users/admin", "tier", "premium")))
            .then(extensionLabelRepository.insert(
                createLabel("/registry/v1alpha1/users/editor", "role", "editor")))
            .then(extensionLabelRepository.deleteByExtensionName(
                "/registry/v1alpha1/users/admin"))
            .then(extensionLabelRepository.count())
            .as(StepVerifier::create)
            .expectNext(1L)
            .verifyComplete();
    }

    @Test
    void shouldDeleteAllExtensionLabels() {
        extensionLabelRepository.insert(
                createLabel("/registry/v1alpha1/users/admin", "role", "admin"))
            .then(extensionLabelRepository.insert(
                createLabel("/registry/v1alpha1/users/editor", "role", "editor")))
            .then(extensionLabelRepository.deleteAll())
            .then(extensionLabelRepository.count())
            .as(StepVerifier::create)
            .expectNext(0L)
            .verifyComplete();
    }

    // ---- UserRoleRepository tests ----

    @Test
    void shouldInsertAndFindUserRole() {
        var role = createRole("/registry/v1alpha1/users/admin", "super-role");

        userRoleRepository.insert(role)
            .as(StepVerifier::create)
            .expectNextMatches(saved -> saved.getId().getRoleName().equals("super-role"))
            .verifyComplete();
    }

    @Test
    void shouldFindStoreNamesByRoleName() {
        userRoleRepository.insert(
                createRole("/registry/v1alpha1/users/admin", "super-role"))
            .then(userRoleRepository.insert(
                createRole("/registry/v1alpha1/users/editor", "super-role")))
            .then(userRoleRepository.insert(
                createRole("/registry/v1alpha1/users/guest", "viewer")))
            .thenMany(userRoleRepository.findStoreNamesByRoleName("super-role"))
            .as(StepVerifier::create)
            .expectNextCount(2)
            .verifyComplete();
    }

    @Test
    void shouldFindStoreNamesByRoleNameIn() {
        userRoleRepository.insert(
                createRole("/registry/v1alpha1/users/admin", "super-role"))
            .then(userRoleRepository.insert(
                createRole("/registry/v1alpha1/users/editor", "editor")))
            .then(userRoleRepository.insert(
                createRole("/registry/v1alpha1/users/guest", "viewer")))
            .thenMany(userRoleRepository.findStoreNamesByRoleNameIn(
                List.of("super-role", "viewer")))
            .as(StepVerifier::create)
            .expectNextCount(2)
            .verifyComplete();
    }

    @Test
    void shouldDeleteByStoreName() {
        userRoleRepository.insert(
                createRole("/registry/v1alpha1/users/admin", "super-role"))
            .then(userRoleRepository.insert(
                createRole("/registry/v1alpha1/users/editor", "editor")))
            .then(userRoleRepository.deleteByStoreName(
                "/registry/v1alpha1/users/admin"))
            .then(userRoleRepository.count())
            .as(StepVerifier::create)
            .expectNext(1L)
            .verifyComplete();
    }

    @Test
    void shouldDeleteAllUserRoles() {
        userRoleRepository.insert(
                createRole("/registry/v1alpha1/users/admin", "super-role"))
            .then(userRoleRepository.insert(
                createRole("/registry/v1alpha1/users/editor", "editor")))
            .then(userRoleRepository.deleteAll())
            .then(userRoleRepository.count())
            .as(StepVerifier::create)
            .expectNext(0L)
            .verifyComplete();
    }

    // ---- Helpers ----

    private UserPo createUser(String name, String displayName, String email) {
        var user = new UserPo();
        user.setName(name);
        user.setDisplayName(displayName);
        user.setEmail(email);
        user.setCreationTimestamp(Instant.now());
        return user;
    }

    private ExtensionLabel createLabel(String extName, String key, String value) {
        var label = new ExtensionLabel();
        var id = new ExtensionLabelId();
        id.setExtensionName(extName);
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
