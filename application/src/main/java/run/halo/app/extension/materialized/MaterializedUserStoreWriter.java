package run.halo.app.extension.materialized;

import com.fasterxml.jackson.core.type.TypeReference;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.User;
import run.halo.app.extension.ExtensionStoreUtil;
import run.halo.app.extension.MetadataOperator;
import run.halo.app.extension.SchemeManager;
import run.halo.app.infra.utils.JsonUtils;

/**
 * Writer that keeps the materialized tables ({@code users}, {@code extension_labels},
 * {@code user_roles}) in sync when User extensions are created, updated, or deleted.
 *
 * @author halo
 * @since 2.21.0
 */
@Component
public class MaterializedUserStoreWriter {

    private final UserRepository userRepository;
    private final ExtensionLabelRepository labelRepository;
    private final UserRoleRepository roleRepository;
    private final SchemeManager schemeManager;

    public MaterializedUserStoreWriter(UserRepository userRepository,
                                       ExtensionLabelRepository labelRepository,
                                       UserRoleRepository roleRepository,
                                       SchemeManager schemeManager) {
        this.userRepository = userRepository;
        this.labelRepository = labelRepository;
        this.roleRepository = roleRepository;
        this.schemeManager = schemeManager;
    }

    /**
     * Sync a User extension to the materialized tables.
     * <ul>
     *   <li>Upsert into {@code users} table</li>
     *   <li>Replace all labels in {@code extension_labels} for this user</li>
     *   <li>Replace all roles in {@code user_roles} for this user</li>
     * </ul>
     *
     * @param user the User extension to sync
     * @return a {@link Mono} that completes when all operations are done
     */
    public Mono<Void> syncUser(User user) {
        var name = user.getMetadata().getName();
        var storeName = buildStoreName(name);
        var userPo = toUserPo(user);
        var labels = user.getMetadata().getLabels();
        var roles = extractRoles(user);
        return upsertUser(userPo)
            .then(replaceLabels(storeName, labels))
            .then(replaceRoles(storeName, roles));
    }

    /**
     * Delete a user from all materialized tables.
     *
     * @param storeName the store name of the user to delete
     * @return a {@link Mono} that completes when all deletions are done
     */
    public Mono<Void> deleteUser(String storeName) {
        return userRepository.deleteById(storeName)
            .then(labelRepository.deleteByExtensionName(storeName))
            .then(roleRepository.deleteByStoreName(storeName));
    }

    private String buildStoreName(String name) {
        var scheme = schemeManager.get(User.class);
        return ExtensionStoreUtil.buildStoreName(scheme, name);
    }

    private UserPo toUserPo(User user) {
        var po = new UserPo();
        po.setName(user.getMetadata().getName());
        po.setDisplayName(user.getSpec().getDisplayName());
        po.setEmail(user.getSpec().getEmail());
        po.setEmailVerified(user.getSpec().isEmailVerified());
        po.setDisabled(Boolean.TRUE.equals(user.getSpec().getDisabled()));
        po.setCreationTimestamp(user.getMetadata().getCreationTimestamp());
        po.setDeletionTimestamp(user.getMetadata().getDeletionTimestamp());
        po.setVersion(user.getMetadata().getVersion());
        return po;
    }

    private Set<String> extractRoles(User user) {
        return Optional.ofNullable(user.getMetadata())
            .map(MetadataOperator::getAnnotations)
            .map(annotations -> annotations.get(User.ROLE_NAMES_ANNO))
            .filter(StringUtils::hasText)
            .map(json -> JsonUtils.jsonToObject(json, new TypeReference<Set<String>>() {}))
            .orElse(Set.of());
    }

    private Mono<Void> replaceLabels(String storeName, Map<String, String> labels) {
        if (labels == null || labels.isEmpty()) {
            return labelRepository.deleteByExtensionName(storeName);
        }
        return labelRepository.deleteByExtensionName(storeName)
            .thenMany(Flux.fromIterable(labels.entrySet()))
            .flatMap(entry -> {
                var label = new ExtensionLabel();
                var id = new ExtensionLabelId();
                id.setExtensionName(storeName);
                id.setLabelKey(entry.getKey());
                label.setId(id);
                label.setLabelValue(entry.getValue());
                return labelRepository.insert(label);
            })
            .then();
    }

    private Mono<Void> replaceRoles(String storeName, Set<String> roles) {
        if (roles.isEmpty()) {
            return roleRepository.deleteByStoreName(storeName);
        }
        return roleRepository.deleteByStoreName(storeName)
            .thenMany(Flux.fromIterable(roles))
            .flatMap(roleName -> {
                var role = new UserRole();
                var id = new UserRoleId();
                id.setStoreName(storeName);
                id.setRoleName(roleName);
                role.setId(id);
                return roleRepository.insert(role);
            })
            .then();
    }

    private Mono<Void> upsertUser(UserPo userPo) {
        // R2DBC save() with @Version always tries UPDATE first when @Id is set,
        // which fails with OptimisticLockingFailureException if the row doesn't exist yet.
        // To work around this, we check existence first:
        // - If found, copy the existing version so UPDATE matches correctly.
        // - If not found, null out the version so R2DBC treats it as a new INSERT.
        return userRepository.existsById(userPo.getName())
            .flatMap(exists -> {
                if (exists) {
                    return userRepository.findById(userPo.getName())
                        .map(existing -> {
                            userPo.setVersion(existing.getVersion());
                            return userPo;
                        })
                        .flatMap(userRepository::save);
                }
                userPo.setVersion(null);
                return userRepository.save(userPo);
            })
            .then();
    }
}
