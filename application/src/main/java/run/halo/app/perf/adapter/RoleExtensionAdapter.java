package run.halo.app.perf.adapter;

import static java.util.Objects.requireNonNullElse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import io.r2dbc.postgresql.util.Assert;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
import run.halo.app.extension.Metadata;
import run.halo.app.extension.Unstructured;
import run.halo.app.perf.converter.SetConverters;
import run.halo.app.perf.entity.PermissionEntity;
import run.halo.app.perf.entity.RoleEntity;
import run.halo.app.perf.repository.PermissionEntityRepository;
import run.halo.app.perf.repository.RoleEntityRepository;
import run.halo.app.perf.service.LabelService;

// @Component
class RoleExtensionAdapter implements ExtensionAdapter {

    private static final GroupVersionKind ROLE_GVK = GroupVersionKind.fromExtension(Role.class);
    private static final GroupKind PERMISSION_GK = new GroupKind(Role.GROUP, "Permission");

    private final RoleEntityRepository roleEntityRepository;

    private final PermissionEntityRepository permissionEntityRepository;

    private final LabelService labelService;

    private final ReactiveTransactionManager txManager;

    RoleExtensionAdapter(RoleEntityRepository roleEntityRepository,
        PermissionEntityRepository permissionEntityRepository,
        LabelService labelService,
        ReactiveTransactionManager txManager) {
        this.roleEntityRepository = roleEntityRepository;
        this.permissionEntityRepository = permissionEntityRepository;
        this.labelService = labelService;
        this.txManager = txManager;
    }

    @Override
    public boolean support(GroupVersionKind gvk) {
        return Objects.equals(ROLE_GVK.groupKind(), gvk.groupKind());
    }

    @Override
    public <E extends Extension> Mono<E> create(E extension) {
        var role = asRole(extension);
        var labels = role.getMetadata().getLabels();
        var tx = TransactionalOperator.create(txManager);
        var isRoleTemplate = Boolean.parseBoolean(labels.get(Role.TEMPLATE_LABEL_NAME));
        if (isRoleTemplate) {
            // create permission
            var permissionEntity = toPermission(role);
            permissionEntity.markAsNew();
            return permissionEntityRepository.save(permissionEntity)
                .flatMap(created -> labelService.saveLabels(
                            PERMISSION_GK, created.getId(), extension.getMetadata().getLabels()
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
        // create role
        var roleEntity = toRoleEntity(role);
        roleEntity.markAsNew();
        return roleEntityRepository.save(roleEntity)
            .flatMap(created -> labelService.saveLabels(
                        ROLE_GVK.groupKind(), created.getId(),
                        extension.getMetadata().getLabels()
                    )
                    .thenReturn(created)
            )
            .as(tx::transactional)
            .doOnNext(created -> {
                extension.getMetadata().setCreationTimestamp(created.getCreatedDate().orElse(null));
                extension.getMetadata().setVersion(created.getVersion());
            })
            .thenReturn(extension);
    }

    @Override
    public <E extends Extension> Mono<E> update(E extension) {
        var role = asRole(extension);
        var labels = role.getMetadata().getLabels();
        var tx = TransactionalOperator.create(txManager);
        var isRoleTemplate = Boolean.parseBoolean(labels.get(Role.TEMPLATE_LABEL_NAME));
        if (isRoleTemplate) {
            // update permission
            return permissionEntityRepository.findById(extension.getMetadata().getName())
                .doOnNext(permissionToUpdate -> {
                    updatePermissionEntity(permissionToUpdate, role);
                })
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

        // update role
        return roleEntityRepository.findById(extension.getMetadata().getName())
            .doOnNext(roleToUpdate -> {
                updateRoleEntity(roleToUpdate, role);
            })
            .flatMap(roleEntityRepository::save)
            .flatMap(updated -> labelService.saveLabels(
                        ROLE_GVK.groupKind(), updated.getId(), role.getMetadata().getLabels()
                    )
                    .thenReturn(updated)
            )
            .as(tx::transactional)
            .doOnNext(updated -> extension.getMetadata().setVersion(updated.getVersion()))
            .thenReturn(extension);
    }

    @Override
    public <E extends Extension> Mono<E> findById(String id) {
        // TODO Only find permission
        return permissionEntityRepository.findById(id)
            .zipWith(labelService.getLabels(PERMISSION_GK, id), (permission, labels) -> {
                var role = permissionToRole(permission);
                if (!CollectionUtils.isEmpty(labels)) {
                    if (role.getMetadata().getLabels() == null) {
                        role.getMetadata().setLabels(new HashMap<>());
                    }
                    role.getMetadata().getLabels().putAll(labels);
                }
                return (E) role;
            });
    }

    @Override
    public <E extends Extension> Flux<E> findAll() {
        // TODO Only find permissions
        return null;
    }

    @Override
    public <E extends Extension> Flux<E> findAll(ListOptions options, Sort sort) {
        return null;
    }

    @Override
    public <E extends Extension> Mono<ListResult<E>> pageBy(ListOptions options,
        Pageable pageable) {
        return null;
    }

    private static boolean isRoleTemplate(Role role) {
        var labels = role.getMetadata().getLabels();
        return labels != null && Boolean.parseBoolean(labels.get(Role.TEMPLATE_LABEL_NAME));
    }

    private PermissionEntity toPermission(Role roleTemplate) {
        Assert.isTrue(isRoleTemplate(roleTemplate),
            "Role is not a template: " + roleTemplate.getMetadata().getName()
        );
        var entity = new PermissionEntity();
        updatePermissionEntity(entity, roleTemplate);
        return entity;
    }

    private Role permissionToRole(PermissionEntity entity) {
        var role = new Role();
        role.setMetadata(new Metadata());
        role.getMetadata().setAnnotations(new HashMap<>());
        role.getMetadata().setLabels(new HashMap<>());

        // handle metadata
        role.getMetadata().setName(entity.getId());
        role.getMetadata().setCreationTimestamp(entity.getCreatedDate());
        role.getMetadata().setVersion(entity.getVersion());
        role.getMetadata().setDeletionTimestamp(entity.getDeletedDate());

        // handle annotations and labels
        if (!CollectionUtils.isEmpty(entity.getFinalizers())) {
            role.getMetadata().setFinalizers(new HashSet<>(entity.getFinalizers()));
        }

        if (entity.getAnnotations() != null) {
            role.getMetadata().getAnnotations().putAll(entity.getAnnotations());
        }
        role.getMetadata().getLabels().put(Role.TEMPLATE_LABEL_NAME, Boolean.TRUE.toString());

        try {
            if (!CollectionUtils.isEmpty(entity.getUiPermissions())) {
                role.getMetadata().getAnnotations().put(
                    Role.UI_PERMISSIONS_ANNO,
                    SetConverters.MAPPER.writeValueAsString(entity.getUiPermissions())
                );
            }
            if (!CollectionUtils.isEmpty(entity.getDependencies())) {
                role.getMetadata().getAnnotations().put(
                    Role.ROLE_DEPENDENCIES_ANNO,
                    SetConverters.MAPPER.writeValueAsString(entity.getDescription())
                );
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        if (StringUtils.hasText(entity.getCategory())) {
            role.getMetadata().getAnnotations().put(Role.MODULE_ANNO, entity.getCategory());
        }

        // handle rules
        if (!CollectionUtils.isEmpty(entity.getRules())) {
            role.setRules(new ArrayList<>(entity.getRules()));
        }

        return role;
    }

    private void updatePermissionEntity(PermissionEntity entity, Role roleTemplate) {
        var metadata = roleTemplate.getMetadata();
        var annotations = new HashMap<>(requireNonNullElse(metadata.getAnnotations(), Map.of()));
        entity.setId(metadata.getName());
        entity.setDisplayName(annotations.get(Role.DISPLAY_NAME_ANNO));
        entity.setCategory(annotations.get(Role.MODULE_ANNO));
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
        var rules = roleTemplate.getRules();
        if (rules != null) {
            entity.setRules(new HashSet<>(rules));
        }
        metadata.setAnnotations(annotations);
    }

    private Role asRole(Extension e) {
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

    private RoleEntity toRoleEntity(Role role) {
        Assert.isTrue(!isRoleTemplate(role), "Role must not be a template: "
            + role.getMetadata().getName()
        );
        var entity = new RoleEntity();
        updateRoleEntity(entity, role);
        return entity;
    }

    private void updateRoleEntity(RoleEntity entity, Role role) {
        var metadata = role.getMetadata();
        var annotations = new HashMap<>(requireNonNullElse(metadata.getAnnotations(), Map.of()));
        var labels = metadata.getLabels();

        entity.setId(metadata.getName());
        entity.setDisplayName(annotations.get(Role.DISPLAY_NAME_ANNO));
        entity.setFinalizers(metadata.getFinalizers());
        entity.setAnnotations(annotations);
        entity.setReserved(
            labels != null && Boolean.parseBoolean(labels.get(Role.SYSTEM_RESERVED_LABELS))
        );
        entity.setDeletedDate(metadata.getDeletionTimestamp());
        if (metadata.getCreationTimestamp() != null) {
            entity.setCreatedDate(metadata.getCreationTimestamp());
        }
    }

}
