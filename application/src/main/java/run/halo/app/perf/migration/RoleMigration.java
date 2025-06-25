package run.halo.app.perf.migration;

import static java.util.Objects.requireNonNullElse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import io.r2dbc.postgresql.util.Assert;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.Role;
import run.halo.app.extension.ExtensionConverter;
import run.halo.app.extension.ExtensionStoreUtil;
import run.halo.app.extension.GroupKind;
import run.halo.app.extension.Scheme;
import run.halo.app.extension.store.ReactiveExtensionStoreClient;
import run.halo.app.perf.converter.SetConverters;
import run.halo.app.perf.entity.PermissionEntity;
import run.halo.app.perf.entity.RoleEntity;
import run.halo.app.perf.entity.RolePermissionEntity;
import run.halo.app.perf.repository.PermissionEntityRepository;
import run.halo.app.perf.repository.RoleEntityRepository;
import run.halo.app.perf.repository.RolePermissionEntityRepository;
import run.halo.app.perf.service.LabelService;
import run.halo.app.security.authorization.AuthorityUtils;

@Slf4j
@Component
class RoleMigration implements ExtensionMigration {

    private static final GroupKind ROLE_GK = new GroupKind(Role.GROUP, "Role");
    private static final GroupKind PERMISSION_GK = new GroupKind(Role.GROUP, "Permission");

    private final RoleEntityRepository roleEntityRepository;

    private final PermissionEntityRepository permissionEntityRepository;

    private final RolePermissionEntityRepository rolePermissionEntityRepository;

    private final ReactiveExtensionStoreClient storeClient;

    private final LabelService labelService;

    private final ExtensionConverter converter;

    private final ReactiveTransactionManager txManager;

    public RoleMigration(RoleEntityRepository roleEntityRepository,
        PermissionEntityRepository permissionEntityRepository,
        RolePermissionEntityRepository rolePermissionEntityRepository,
        ReactiveExtensionStoreClient storeClient,
        LabelService labelService,
        ExtensionConverter converter,
        ReactiveTransactionManager txManager) {
        this.roleEntityRepository = roleEntityRepository;
        this.permissionEntityRepository = permissionEntityRepository;
        this.rolePermissionEntityRepository = rolePermissionEntityRepository;
        this.storeClient = storeClient;
        this.labelService = labelService;
        this.converter = converter;
        this.txManager = txManager;
    }

    @Override
    public boolean support(Scheme scheme) {
        return Role.class == scheme.type();
    }

    @Override
    public Mono<Void> migrate(Scheme scheme) {
        // detect if roles have been migrated
        return roleEntityRepository.count()
            .filter(count -> count == 0)
            .switchIfEmpty(
                Mono.fromRunnable(() -> log.info(
                    "Already migrated roles to new store, skip migration for scheme: {}",
                    scheme.groupVersionKind())).then(Mono.empty())
            )
            .flatMap(ignored -> {
                var prefix = ExtensionStoreUtil.buildStoreNamePrefix(scheme);
                var tx = TransactionalOperator.create(txManager);
                return storeClient.listByNamePrefix(prefix)
                    .limitRate(100)
                    .map(store -> converter.convertFrom(Role.class, store))
                    .flatMap(role -> {
                        if (Role.isRoleTemplate(role)) {
                            // convert it to a permission
                            var permission = toPermission(role);
                            permission.markAsNew();
                            return permissionEntityRepository.save(permission)
                                .flatMap(created -> labelService.saveLabels(
                                            PERMISSION_GK,
                                            created.getId(),
                                            role.getMetadata().getLabels()
                                        )
                                        .thenReturn(created)
                                )
                                .doOnSuccess(created -> log.info(
                                    "Migrated role template to permission: {}", created.getId()
                                ))
                                .then();
                        }
                        var roleEntity = toRoleEntity(role);
                        roleEntity.markAsNew();
                        if (Objects.equals(
                            AuthorityUtils.SUPER_ROLE_NAME, role.getMetadata().getName()
                        )) {
                            // create role and permission
                            // bind role nad permission
                            var superPermission = toPermission(role);
                            superPermission.setId("super-permission");
                            superPermission.markAsNew();
                            // no need to save labels for the super permission
                            var savePermission = permissionEntityRepository.save(superPermission);
                            // no need to save labels for the super role
                            var saveRole = roleEntityRepository.save(roleEntity);

                            var rp = new RolePermissionEntity();
                            rp.setPermissionId("super-permission");
                            rp.setRoleId(role.getMetadata().getName());
                            var saveRolePermission = rolePermissionEntityRepository.save(rp);
                            return Mono.when(savePermission, saveRole, saveRolePermission);
                        }
                        return roleEntityRepository.save(roleEntity)
                            .flatMap(created -> labelService.saveLabels(
                                        ROLE_GK, created.getId(), role.getMetadata().getLabels()
                                    )
                                    .thenReturn(created)
                            )
                            .doOnSuccess(created -> log.info(
                                "Migrated role to role entity: {}", created.getId()
                            ))
                            .flatMap(created -> {
                                var annotations = requireNonNullElse(
                                    role.getMetadata().getAnnotations(), Map.<String, String>of()
                                );
                                var dependenciesJson = annotations.get(Role.ROLE_DEPENDENCIES_ANNO);
                                if (!StringUtils.hasText(dependenciesJson)) {
                                    return Mono.empty();
                                }
                                Set<String> dependencies;
                                try {
                                    dependencies = SetConverters.MAPPER.readValue(dependenciesJson,
                                        new TypeReference<>() {
                                        });
                                } catch (JsonProcessingException e) {
                                    return Mono.error(e);
                                }
                                if (CollectionUtils.isEmpty(dependencies)) {
                                    return Mono.empty();
                                }
                                log.info(
                                    "Creating role-permission for role: {} with dependencies: {}",
                                    created.getId(), dependencies
                                );
                                var rolePermissions = dependencies.stream()
                                    .map(dependency -> {
                                        var entity = new RolePermissionEntity();
                                        entity.setRoleId(created.getId());
                                        entity.setPermissionId(dependency);
                                        return entity;
                                    })
                                    .collect(Collectors.toSet());
                                return rolePermissionEntityRepository.saveAll(rolePermissions)
                                    .then()
                                    .doOnSuccess(
                                        v -> log.info("Created role-permission for role: {}",
                                            created.getId())
                                    )
                                    .thenReturn(created);
                            })
                            .then();
                    })
                    .as(tx::transactional)
                    .then();
            });
    }

    private static RoleEntity toRoleEntity(Role role) {
        Assert.isTrue(!Role.isRoleTemplate(role), "Role must not be a template: "
            + role.getMetadata().getName()
        );
        var entity = new RoleEntity();
        updateRoleEntity(entity, role);
        return entity;
    }

    private static PermissionEntity toPermission(Role role) {
        var entity = new PermissionEntity();
        updatePermissionEntity(entity, role);
        return entity;
    }

    private static void updateRoleEntity(RoleEntity entity, Role role) {
        var metadata = role.getMetadata();
        var annotations = new HashMap<>(requireNonNullElse(metadata.getAnnotations(), Map.of()));
        var labels = requireNonNullElse(metadata.getLabels(), Map.<String, String>of());

        entity.setId(metadata.getName());
        entity.setDisplayName(annotations.get(Role.DISPLAY_NAME_ANNO));
        entity.setFinalizers(metadata.getFinalizers());
        entity.setAnnotations(annotations);
        entity.setReserved(Boolean.parseBoolean(labels.get(Role.SYSTEM_RESERVED_LABELS)));
        entity.setDeletedDate(metadata.getDeletionTimestamp());
        if (metadata.getCreationTimestamp() != null) {
            entity.setCreatedDate(metadata.getCreationTimestamp());
        }
    }

    private static void updatePermissionEntity(PermissionEntity entity, Role role) {
        var metadata = role.getMetadata();
        var annotations =
            new HashMap<>(requireNonNullElse(metadata.getAnnotations(), Map.of()));
        entity.setId(metadata.getName());
        entity.setDisplayName(annotations.get(Role.DISPLAY_NAME_ANNO));
        entity.setCategory(annotations.get(Role.MODULE_ANNO));

        var labels = requireNonNullElse(metadata.getLabels(), Map.<String, String>of());
        var hiddenLabel = labels.get(Role.HIDDEN_LABEL_NAME);
        entity.setHidden(Boolean.parseBoolean(hiddenLabel));

        var uiPermissionsJson = annotations.get(Role.UI_PERMISSIONS_ANNO);
        if (StringUtils.hasText(uiPermissionsJson)) {
            try {
                entity.setUiPermissions(
                    SetConverters.MAPPER.readValue(uiPermissionsJson, new TypeReference<>() {
                    })
                );
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }

        var dependenciesJson = annotations.get(Role.ROLE_DEPENDENCIES_ANNO);
        if (StringUtils.hasText(dependenciesJson)) {
            try {
                entity.setDependencies(
                    SetConverters.MAPPER.readValue(dependenciesJson, new TypeReference<>() {
                    })
                );
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        entity.setAnnotations(annotations);
        entity.setFinalizers(metadata.getFinalizers());
        entity.setDeletedDate(metadata.getDeletionTimestamp());
        if (metadata.getCreationTimestamp() != null) {
            entity.setCreatedDate(metadata.getCreationTimestamp());
        }
        var rules = role.getRules();
        if (rules != null) {
            entity.setRules(new HashSet<>(rules));
        }
        metadata.setAnnotations(annotations);
    }
}
