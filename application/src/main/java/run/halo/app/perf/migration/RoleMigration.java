package run.halo.app.perf.migration;

import static java.util.Objects.requireNonNullElse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.Map;
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
import run.halo.app.perf.adapter.RoleExtensionAdapter;
import run.halo.app.perf.converter.SetConverters;
import run.halo.app.perf.entity.RolePermissionEntity;
import run.halo.app.perf.repository.PermissionEntityRepository;
import run.halo.app.perf.repository.RoleEntityRepository;
import run.halo.app.perf.repository.RolePermissionEntityRepository;
import run.halo.app.perf.service.LabelService;

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
                            var permission = RoleExtensionAdapter.toPermission(role);
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
                        var roleEntity = RoleExtensionAdapter.toRoleEntity(role);
                        roleEntity.markAsNew();
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

}
