package run.halo.app.perf.adapter;

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
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.Role;
import run.halo.app.extension.Extension;
import run.halo.app.extension.GroupKind;
import run.halo.app.extension.GroupVersionKind;
import run.halo.app.extension.JsonExtension;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.ListResult;
import run.halo.app.extension.Unstructured;
import run.halo.app.extension.exception.ExtensionNotFoundException;
import run.halo.app.perf.converter.SetConverters;
import run.halo.app.perf.entity.PermissionEntity;
import run.halo.app.perf.entity.RoleEntity;
import run.halo.app.perf.entity.RolePermissionEntity;
import run.halo.app.perf.repository.PermissionEntityRepository;
import run.halo.app.perf.repository.RoleEntityRepository;
import run.halo.app.perf.repository.RolePermissionEntityRepository;
import run.halo.app.perf.service.LabelService;

@Component
public class RoleExtensionAdapter implements ExtensionAdapter {

    private static final GroupVersionKind ROLE_GVK = GroupVersionKind.fromExtension(Role.class);
    private static final GroupKind PERMISSION_GK = new GroupKind(Role.GROUP, "Permission");

    private final RoleEntityRepository roleEntityRepository;

    private final PermissionEntityRepository permissionEntityRepository;

    private final RolePermissionEntityRepository rolePermissionEntityRepository;

    private final LabelService labelService;

    private final ReactiveTransactionManager txManager;

    RoleExtensionAdapter(RoleEntityRepository roleEntityRepository,
        PermissionEntityRepository permissionEntityRepository,
        RolePermissionEntityRepository rolePermissionEntityRepository,
        LabelService labelService,
        ReactiveTransactionManager txManager) {
        this.roleEntityRepository = roleEntityRepository;
        this.permissionEntityRepository = permissionEntityRepository;
        this.rolePermissionEntityRepository = rolePermissionEntityRepository;
        this.labelService = labelService;
        this.txManager = txManager;
    }

    public static PermissionEntity toPermission(Role role) {
        var entity = new PermissionEntity();
        updatePermissionEntity(entity, role);
        return entity;
    }

    public static RoleEntity toRoleEntity(Role role) {
        Assert.isTrue(!Role.isRoleTemplate(role), "Role must not be a template: "
            + role.getMetadata().getName()
        );
        var entity = new RoleEntity();
        updateRoleEntity(entity, role);
        return entity;
    }

    public static void updatePermissionEntity(PermissionEntity entity, Role role) {
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

    public static void updateRoleEntity(RoleEntity entity, Role role) {
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

    @Override
    public boolean support(GroupVersionKind gvk) {
        return Objects.equals(ROLE_GVK.groupKind(), gvk.groupKind());
    }

    @Override
    public <E extends Extension> Mono<E> create(E extension) {
        return Mono.error(new UnsupportedOperationException(
            "Creating role is not supported: " + extension.getMetadata().getName()
        ));

        // var role = asRole(extension);
        // var labels = role.getMetadata().getLabels();
        // var tx = TransactionalOperator.create(txManager);
        // if (isRoleTemplate(role)) {
        //     // create permission
        //     var permissionEntity = toPermission(role);
        //     permissionEntity.markAsNew();
        //     return permissionEntityRepository.save(permissionEntity)
        //         .flatMap(created -> labelService.saveLabels(
        //                     PERMISSION_GK, created.getId(), extension.getMetadata().getLabels()
        //                 )
        //                 .thenReturn(created)
        //         )
        //         .as(tx::transactional)
        //         .doOnNext(created -> {
        //             extension.getMetadata().setCreationTimestamp(created.getCreatedDate());
        //             extension.getMetadata().setVersion(created.getVersion());
        //         })
        //         .thenReturn(extension);
        // }
        // // create role
        // var roleEntity = toRoleEntity(role);
        // roleEntity.markAsNew();
        // return roleEntityRepository.save(roleEntity)
        //     .flatMap(created -> labelService.saveLabels(
        //                 ROLE_GVK.groupKind(), created.getId(),
        //                 extension.getMetadata().getLabels()
        //             )
        //             .thenReturn(created)
        //     )
        //     .as(tx::transactional)
        //     .doOnNext(created -> {
        //         extension.getMetadata().setCreationTimestamp(created.getCreatedDate().orElse
        //         (null));
        //         extension.getMetadata().setVersion(created.getVersion());
        //     })
        //     .thenReturn(extension);
    }

    @Override
    public <E extends Extension> Mono<E> update(E extension) {
        return Mono.error(new UnsupportedOperationException(
            "Updating role is not supported: " + extension.getMetadata().getName()
        ));

        // var role = asRole(extension);
        // var labels = role.getMetadata().getLabels();
        // var tx = TransactionalOperator.create(txManager);
        // var isRoleTemplate = Boolean.parseBoolean(labels.get(Role.TEMPLATE_LABEL_NAME));
        // if (isRoleTemplate) {
        //     // update permission
        //     return permissionEntityRepository.findById(extension.getMetadata().getName())
        //         .doOnNext(permissionToUpdate -> {
        //             updatePermissionEntity(permissionToUpdate, role);
        //         })
        //         .flatMap(permissionEntityRepository::save)
        //         .flatMap(updated -> labelService.saveLabels(
        //                     PERMISSION_GK, updated.getId(), role.getMetadata().getLabels()
        //                 )
        //                 .thenReturn(updated)
        //         )
        //         .as(tx::transactional)
        //         .doOnNext(updated -> extension.getMetadata().setVersion(updated.getVersion()))
        //         .thenReturn(extension);
        // }
        //
        // // update role
        // return roleEntityRepository.findById(extension.getMetadata().getName())
        //     .doOnNext(roleToUpdate -> {
        //         updateRoleEntity(roleToUpdate, role);
        //     })
        //     .flatMap(roleEntityRepository::save)
        //     .flatMap(updated -> labelService.saveLabels(
        //                 ROLE_GVK.groupKind(), updated.getId(), role.getMetadata().getLabels()
        //             )
        //             .thenReturn(updated)
        //     )
        //     .as(tx::transactional)
        //     .doOnNext(updated -> extension.getMetadata().setVersion(updated.getVersion()))
        //     .thenReturn(extension);
    }

    @Override
    public <E extends Extension> Mono<E> findById(String id) {
        return Mono.error(new UnsupportedOperationException(
            "Finding role by ID is not supported: " + id
        ));
        // // TODO Only find permission
        // return permissionEntityRepository.findById(id)
        //     .zipWith(labelService.getLabels(PERMISSION_GK, id), (permission, labels) -> {
        //         var role = permissionToRole(permission);
        //         if (!CollectionUtils.isEmpty(labels)) {
        //             if (role.getMetadata().getLabels() == null) {
        //                 role.getMetadata().setLabels(new HashMap<>());
        //             }
        //             role.getMetadata().getLabels().putAll(labels);
        //         }
        //         return (E) role;
        //     });
    }

    @Override
    public <E extends Extension> Flux<E> findAll() {
        return Flux.error(new UnsupportedOperationException(
            "Finding all roles is not supported."
        ));
    }

    @Override
    public <E extends Extension> Flux<E> findAll(ListOptions options, Sort sort) {
        return Flux.error(new UnsupportedOperationException(
            "Finding all roles with options is not supported: " + options + ", " + sort
        ));
    }

    @Override
    public <E extends Extension> Mono<ListResult<E>> pageBy(ListOptions options,
        Pageable pageable) {
        return Mono.error(new UnsupportedOperationException(
            "Paging roles is not supported: " + options + ", " + pageable
        ));
    }

    @Override
    public <E extends Extension> Mono<E> initialize(E extension) {
        var role = asRole(extension);
        if (Role.isRoleTemplate(role)) {
            // create or update permission
            return updatePermission(extension)
                .onErrorResume(ExtensionNotFoundException.class, e -> createPermission(extension));
        } else {
            // create o update role
            return updateRole(extension)
                .onErrorResume(ExtensionNotFoundException.class, e -> createRole(extension));
        }
    }

    private <E extends Extension> Mono<E> updatePermission(E extension) {
        var role = asRole(extension);
        var tx = TransactionalOperator.create(txManager);
        // create permission
        return permissionEntityRepository.findById(extension.getMetadata().getName())
            .switchIfEmpty(Mono.error(() -> new ExtensionNotFoundException(
                extension.groupVersionKind(), extension.getMetadata().getName()
            )))
            .doOnNext(permissionToUpdate -> updatePermissionEntity(permissionToUpdate, role))
            .flatMap(permissionEntityRepository::save)
            .flatMap(updated -> labelService.saveLabels(
                        PERMISSION_GK, updated.getId(), role.getMetadata().getLabels()
                    )
                    .thenReturn(updated)
            )
            .as(tx::transactional)
            .doOnNext(updated -> extension.getMetadata().setVersion(updated.getVersion()))
            .thenReturn(extension);
    }

    private <E extends Extension> Mono<E> updateRole(E extension) {
        var role = asRole(extension);
        var tx = TransactionalOperator.create(txManager);
        // update role
        return roleEntityRepository.findById(extension.getMetadata().getName())
            .switchIfEmpty(Mono.error(() -> new ExtensionNotFoundException(
                extension.groupVersionKind(), extension.getMetadata().getName()
            )))
            .doOnNext(roleToUpdate -> updateRoleEntity(roleToUpdate, role))
            .flatMap(roleEntityRepository::save)
            .flatMap(updated -> labelService.saveLabels(
                        ROLE_GVK.groupKind(), updated.getId(), role.getMetadata().getLabels()
                    )
                    .thenReturn(updated)
            )
            .flatMap(updated -> updateRolePermission(updated, role)
                .thenReturn(updated)
            )
            .as(tx::transactional)
            .doOnNext(updated -> extension.getMetadata().setVersion(updated.getVersion()))
            .thenReturn(extension);
    }

    private <E extends Extension> Mono<E> createRole(E extension) {
        var role = asRole(extension);
        var tx = TransactionalOperator.create(txManager);
        // create role
        var roleEntity = toRoleEntity(role);
        roleEntity.markAsNew();
        return roleEntityRepository.save(roleEntity)
            .flatMap(created -> labelService.saveLabels(
                        ROLE_GVK.groupKind(), created.getId(), role.getMetadata().getLabels()
                    )
                    .thenReturn(created)
            )
            .flatMap(created -> updateRolePermission(created, role)
                .thenReturn(created)
            )
            .as(tx::transactional)
            .doOnNext(created -> {
                extension.getMetadata().setCreationTimestamp(created.getCreatedDate().orElse(null));
                extension.getMetadata().setVersion(created.getVersion());
            })
            .thenReturn(extension);
    }

    private <E extends Extension> Mono<E> createPermission(E extension) {
        var role = asRole(extension);
        var tx = TransactionalOperator.create(txManager);
        // create permission
        var permissionEntity = toPermission(role);
        permissionEntity.markAsNew();
        return permissionEntityRepository.save(permissionEntity)
            .flatMap(created -> labelService.saveLabels(
                        PERMISSION_GK, created.getId(), role.getMetadata().getLabels()
                    )
                    .thenReturn(created)
            )
            .as(tx::transactional)
            .doOnNext(created -> {
                extension.getMetadata().setCreationTimestamp(created.getCreatedDate());
                extension.getMetadata().setVersion(created.getVersion());
            })
            .thenReturn(extension);
    }

    private Mono<Void> updateRolePermission(RoleEntity entity, Role role) {
        var annotations =
            requireNonNullElse(role.getMetadata().getAnnotations(), Map.<String, String>of());
        var dependenciesJson = annotations.get(Role.ROLE_DEPENDENCIES_ANNO);
        if (!StringUtils.hasText(dependenciesJson)) {
            return Mono.empty();
        }
        Set<String> dependencies;
        try {
            dependencies = SetConverters.MAPPER.readValue(dependenciesJson, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            return Mono.error(e);
        }

        if (CollectionUtils.isEmpty(dependencies)) {
            return Mono.empty();
        }
        // remove all dependencies first and add new ones
        var roleId = entity.getId();
        return rolePermissionEntityRepository.findByRoleId(roleId)
            .collectList()
            .flatMap(rps -> {
                var toDelete = rps.stream()
                    .map(RolePermissionEntity::getPermissionId)
                    .collect(Collectors.toSet());
                var toAdd = new HashSet<String>();
                for (var dependency : dependencies) {
                    if (!toDelete.remove(dependency)) {
                        toAdd.add(dependency);
                    }
                }
                var delete = Mono.just(toDelete)
                    .filter(c -> !c.isEmpty())
                    .flatMap(permissionIds ->
                        rolePermissionEntityRepository.deleteByRoleIdAndPermissionIdIn(
                            roleId, toDelete
                        )
                    );
                var add = Mono.just(toAdd)
                    .map(permissionIds -> permissionIds.stream()
                        .map(dependency -> {
                            var rpEntity = new RolePermissionEntity();
                            rpEntity.setRoleId(roleId);
                            rpEntity.setPermissionId(dependency);
                            return rpEntity;
                        })
                        .collect(Collectors.toSet())
                    )
                    .filter(c -> !c.isEmpty())
                    .flatMapMany(rolePermissionEntityRepository::saveAll)
                    .then();
                return Mono.when(delete, add);
            });
    }

    private static Role asRole(Extension e) {
        if (e instanceof Role role) {
            return role;
        }
        if (e instanceof Unstructured) {
            return Unstructured.OBJECT_MAPPER.convertValue(e, Role.class);
        } else if (e instanceof JsonExtension jsonExtension) {
            return jsonExtension.getObjectMapper().convertValue(jsonExtension, Role.class);
        } else {
            throw new IllegalArgumentException(
                "Unsupported extension type: " + e.getClass());
        }
    }

}
