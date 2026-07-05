package run.halo.app.extension.materialized;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.halo.app.core.extension.User;
import run.halo.app.extension.Metadata;
import run.halo.app.extension.SchemeManager;
import run.halo.app.infra.utils.JsonUtils;

/**
 * Tests for {@link MaterializedUserStoreWriter}.
 *
 * @author halo
 * @since 2.21.0
 */
@ExtendWith(MockitoExtension.class)
class MaterializedUserStoreWriterTest {

    @Mock
    UserRepository userRepository;

    @Mock
    ExtensionLabelRepository labelRepository;

    @Mock
    UserRoleRepository roleRepository;

    @Mock
    SchemeManager schemeManager;

    MaterializedUserStoreWriter writer;

    static final String STORE_NAME_PREFIX = "/registry/users";

    @BeforeEach
    void setUp() {
        writer = new MaterializedUserStoreWriter(
            userRepository, labelRepository, roleRepository, schemeManager
        );
    }

    @Test
    void syncUserShouldCreateUserPoLabelsAndRoles() {
        var user = createUser("admin", "Admin User", "admin@example.com");
        user.getMetadata().setLabels(Map.of(
            "app", "halo",
            "env", "prod"
        ));
        user.getMetadata().setAnnotations(Map.of(
            User.ROLE_NAMES_ANNO, JsonUtils.objectToJson(Set.of("role-admin", "role-editor"))
        ));

        when(schemeManager.get(User.class)).thenReturn(buildUserScheme());
        when(userRepository.save(any())).thenReturn(Mono.empty());
        when(labelRepository.deleteByExtensionName(any())).thenReturn(Mono.empty());
        when(labelRepository.insert(any())).thenReturn(Mono.empty());
        when(roleRepository.deleteByStoreName(any())).thenReturn(Mono.empty());
        when(roleRepository.insert(any())).thenReturn(Mono.empty());

        writer.syncUser(user)
            .as(StepVerifier::create)
            .verifyComplete();

        // Verify UserPo was saved
        var userPoCaptor = ArgumentCaptor.forClass(UserPo.class);
        verify(userRepository).save(userPoCaptor.capture());
        var savedPo = userPoCaptor.getValue();
        assertThat(savedPo.getName()).isEqualTo("admin");
        assertThat(savedPo.getDisplayName()).isEqualTo("Admin User");
        assertThat(savedPo.getEmail()).isEqualTo("admin@example.com");

        // Verify labels were inserted (2 labels)
        var labelCaptor = ArgumentCaptor.forClass(ExtensionLabel.class);
        verify(labelRepository).deleteByExtensionName(STORE_NAME_PREFIX + "/admin");
        verify(labelRepository, org.mockito.Mockito.times(2)).insert(labelCaptor.capture());
        assertThat(labelCaptor.getAllValues()).hasSize(2);

        // Verify roles were inserted (2 roles)
        var roleCaptor = ArgumentCaptor.forClass(UserRole.class);
        verify(roleRepository).deleteByStoreName(STORE_NAME_PREFIX + "/admin");
        verify(roleRepository, org.mockito.Mockito.times(2)).insert(roleCaptor.capture());
        assertThat(roleCaptor.getAllValues()).hasSize(2);
    }

    @Test
    void syncUserShouldUpdateExistingUser() {
        var user = createUser("admin", "Updated Admin", "updated@example.com");
        user.getMetadata().setVersion(2L);

        when(schemeManager.get(User.class)).thenReturn(buildUserScheme());
        when(userRepository.save(any())).thenReturn(Mono.empty());
        when(labelRepository.deleteByExtensionName(any())).thenReturn(Mono.empty());
        when(roleRepository.deleteByStoreName(any())).thenReturn(Mono.empty());

        writer.syncUser(user)
            .as(StepVerifier::create)
            .verifyComplete();

        var userPoCaptor = ArgumentCaptor.forClass(UserPo.class);
        verify(userRepository).save(userPoCaptor.capture());
        assertThat(userPoCaptor.getValue().getDisplayName()).isEqualTo("Updated Admin");
        assertThat(userPoCaptor.getValue().getVersion()).isEqualTo(2L);
    }

    @Test
    void syncUserWithNoLabelsShouldDeleteExistingLabels() {
        var user = createUser("admin", "Admin", "admin@example.com");

        when(schemeManager.get(User.class)).thenReturn(buildUserScheme());
        when(userRepository.save(any())).thenReturn(Mono.empty());
        when(labelRepository.deleteByExtensionName(any())).thenReturn(Mono.empty());
        when(roleRepository.deleteByStoreName(any())).thenReturn(Mono.empty());

        writer.syncUser(user)
            .as(StepVerifier::create)
            .verifyComplete();

        verify(labelRepository).deleteByExtensionName(STORE_NAME_PREFIX + "/admin");
        verify(labelRepository, org.mockito.Mockito.never()).insert(any());
    }

    @Test
    void syncUserWithNoRolesShouldDeleteExistingRoles() {
        var user = createUser("admin", "Admin", "admin@example.com");
        // No ROLE_NAMES_ANNO annotation set

        when(schemeManager.get(User.class)).thenReturn(buildUserScheme());
        when(userRepository.save(any())).thenReturn(Mono.empty());
        when(labelRepository.deleteByExtensionName(any())).thenReturn(Mono.empty());
        when(roleRepository.deleteByStoreName(any())).thenReturn(Mono.empty());

        writer.syncUser(user)
            .as(StepVerifier::create)
            .verifyComplete();

        verify(roleRepository).deleteByStoreName(STORE_NAME_PREFIX + "/admin");
        verify(roleRepository, org.mockito.Mockito.never()).insert(any());
    }

    @Test
    void syncUserWithNullLabelsMapShouldHandleGracefully() {
        var user = createUser("admin", "Admin", "admin@example.com");
        // Labels map is null by default from Metadata

        when(schemeManager.get(User.class)).thenReturn(buildUserScheme());
        when(userRepository.save(any())).thenReturn(Mono.empty());
        when(labelRepository.deleteByExtensionName(any())).thenReturn(Mono.empty());
        when(roleRepository.deleteByStoreName(any())).thenReturn(Mono.empty());

        writer.syncUser(user)
            .as(StepVerifier::create)
            .verifyComplete();

        // Should still delete existing labels
        verify(labelRepository).deleteByExtensionName(STORE_NAME_PREFIX + "/admin");
        verify(labelRepository, org.mockito.Mockito.never()).insert(any());
    }

    @Test
    void deleteUserShouldRemoveFromAllTables() {
        var storeName = STORE_NAME_PREFIX + "/admin";

        when(userRepository.deleteById(storeName)).thenReturn(Mono.empty());
        when(labelRepository.deleteByExtensionName(storeName)).thenReturn(Mono.empty());
        when(roleRepository.deleteByStoreName(storeName)).thenReturn(Mono.empty());

        writer.deleteUser(storeName)
            .as(StepVerifier::create)
            .verifyComplete();

        verify(userRepository).deleteById(storeName);
        verify(labelRepository).deleteByExtensionName(storeName);
        verify(roleRepository).deleteByStoreName(storeName);
    }

    @Test
    void syncUserShouldConvertToUserPoCorrectly() {
        var now = Instant.now();
        var user = createUser("testuser", "Test User", "test@example.com");
        user.getSpec().setDisabled(true);
        user.getMetadata().setCreationTimestamp(now);
        user.getMetadata().setDeletionTimestamp(now);
        user.getMetadata().setVersion(5L);

        when(schemeManager.get(User.class)).thenReturn(buildUserScheme());
        when(userRepository.save(any())).thenReturn(Mono.empty());
        when(labelRepository.deleteByExtensionName(any())).thenReturn(Mono.empty());
        when(roleRepository.deleteByStoreName(any())).thenReturn(Mono.empty());

        writer.syncUser(user)
            .as(StepVerifier::create)
            .verifyComplete();

        var userPoCaptor = ArgumentCaptor.forClass(UserPo.class);
        verify(userRepository).save(userPoCaptor.capture());
        var po = userPoCaptor.getValue();
        assertThat(po.getName()).isEqualTo("testuser");
        assertThat(po.getDisplayName()).isEqualTo("Test User");
        assertThat(po.getEmail()).isEqualTo("test@example.com");
        assertThat(po.isEmailVerified()).isFalse();
        assertThat(po.isDisabled()).isTrue();
        assertThat(po.getCreationTimestamp()).isEqualTo(now);
        assertThat(po.getDeletionTimestamp()).isEqualTo(now);
        assertThat(po.getVersion()).isEqualTo(5L);
    }

    @Test
    void syncUserShouldExtractRolesFromAnnotation() {
        var user = createUser("admin", "Admin", "admin@example.com");
        user.getMetadata().setAnnotations(Map.of(
            User.ROLE_NAMES_ANNO, JsonUtils.objectToJson(Set.of("super-admin"))
        ));

        when(schemeManager.get(User.class)).thenReturn(buildUserScheme());
        when(userRepository.save(any())).thenReturn(Mono.empty());
        when(labelRepository.deleteByExtensionName(any())).thenReturn(Mono.empty());
        when(roleRepository.deleteByStoreName(any())).thenReturn(Mono.empty());
        when(roleRepository.insert(any())).thenReturn(Mono.empty());

        writer.syncUser(user)
            .as(StepVerifier::create)
            .verifyComplete();

        var roleCaptor = ArgumentCaptor.forClass(UserRole.class);
        verify(roleRepository).deleteByStoreName(STORE_NAME_PREFIX + "/admin");
        verify(roleRepository).insert(roleCaptor.capture());
        assertThat(roleCaptor.getValue().getId().getRoleName()).isEqualTo("super-admin");
        assertThat(roleCaptor.getValue().getId().getStoreName()).isEqualTo(STORE_NAME_PREFIX + "/admin");
    }

    @Test
    void syncUserShouldReplaceLabelsCorrectly() {
        var user = createUser("admin", "Admin", "admin@example.com");
        user.getMetadata().setLabels(Map.of("tier", "premium"));

        when(schemeManager.get(User.class)).thenReturn(buildUserScheme());
        when(userRepository.save(any())).thenReturn(Mono.empty());
        when(labelRepository.deleteByExtensionName(any())).thenReturn(Mono.empty());
        when(labelRepository.insert(any())).thenReturn(Mono.empty());
        when(roleRepository.deleteByStoreName(any())).thenReturn(Mono.empty());

        writer.syncUser(user)
            .as(StepVerifier::create)
            .verifyComplete();

        var labelCaptor = ArgumentCaptor.forClass(ExtensionLabel.class);
        verify(labelRepository).deleteByExtensionName(STORE_NAME_PREFIX + "/admin");
        verify(labelRepository).insert(labelCaptor.capture());
        var insertedLabel = labelCaptor.getValue();
        assertThat(insertedLabel.getId().getExtensionName()).isEqualTo(STORE_NAME_PREFIX + "/admin");
        assertThat(insertedLabel.getId().getLabelKey()).isEqualTo("tier");
        assertThat(insertedLabel.getLabelValue()).isEqualTo("premium");
    }

    // --- Helper methods ---

    private User createUser(String name, String displayName, String email) {
        var user = new User();
        var metadata = new Metadata();
        metadata.setName(name);
        user.setMetadata(metadata);
        var spec = new User.UserSpec();
        spec.setDisplayName(displayName);
        spec.setEmail(email);
        user.setSpec(spec);
        return user;
    }

    private run.halo.app.extension.Scheme buildUserScheme() {
        return run.halo.app.extension.Scheme.buildFromType(User.class);
    }
}
