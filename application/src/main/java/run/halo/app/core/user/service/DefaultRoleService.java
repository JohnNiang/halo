package run.halo.app.core.user.service;

import static run.halo.app.extension.ExtensionUtil.defaultSort;
import static run.halo.app.extension.ExtensionUtil.notDeleting;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Sort;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.Role;
import run.halo.app.core.extension.RoleBinding;
import run.halo.app.core.extension.RoleBinding.Subject;
import run.halo.app.core.extension.User;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.Metadata;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.extension.index.query.QueryFactory;
import run.halo.app.perf.adapter.RoleExtensionAdapter;
import run.halo.app.perf.entity.PermissionEntity;
import run.halo.app.perf.entity.RoleEntity;
import run.halo.app.perf.entity.RolePermissionEntity;
import run.halo.app.perf.entity.UserRoleEntity;
import run.halo.app.perf.repository.PermissionEntityRepository;
import run.halo.app.perf.repository.RoleEntityRepository;
import run.halo.app.perf.repository.RolePermissionEntityRepository;
import run.halo.app.perf.repository.UserRoleEntityRepository;
import run.halo.app.perf.service.LabelService;
import run.halo.app.perf.util.QueryUtils;
import run.halo.app.security.SuperAdminInitializer;

/**
 * @author guqing
 * @since 2.0.0
 */
@Slf4j
@Service
class DefaultRoleService implements RoleService {

    private final ReactiveExtensionClient client;

    private final UserRoleEntityRepository userRoleEntityRepository;

    private final PermissionEntityRepository permissionEntityRepository;

    private final RolePermissionEntityRepository rolePermissionEntityRepository;

    private final RoleEntityRepository roleEntityRepository;

    private final R2dbcEntityTemplate entityTemplate;

    private final LabelService labelService;

    public DefaultRoleService(ReactiveExtensionClient client,
        UserRoleEntityRepository userRoleEntityRepository,
        PermissionEntityRepository permissionEntityRepository,
        RolePermissionEntityRepository rolePermissionEntityRepository,
        RoleEntityRepository roleEntityRepository,
        R2dbcEntityTemplate entityTemplate, LabelService labelService) {
        this.client = client;
        this.userRoleEntityRepository = userRoleEntityRepository;
        this.permissionEntityRepository = permissionEntityRepository;
        this.rolePermissionEntityRepository = rolePermissionEntityRepository;
        this.roleEntityRepository = roleEntityRepository;
        this.entityTemplate = entityTemplate;
        this.labelService = labelService;
    }

    @Override
    public Flux<RoleBinding> listRoleBindings(Subject subject) {
        Assert.isTrue(Objects.equals(User.KIND, subject.getKind()),
            "Subject kind must be " + User.KIND);
        return userRoleEntityRepository.findByUserId(subject.getName())
            .map(entity -> {
                var rb = new RoleBinding();
                rb.setMetadata(new Metadata());
                rb.getMetadata()
                    .setName(entity.getUserId() + "-" + entity.getRoleId() + "-binding");
                rb.setSubjects(List.of(subject));

                var ref = new RoleBinding.RoleRef();
                ref.setName(entity.getRoleId());
                ref.setKind(Role.KIND);
                ref.setApiGroup(Role.GROUP);
                rb.setRoleRef(ref);
                return rb;
            });
    }

    @Override
    public Flux<String> getRolesByUsername(String username) {
        return userRoleEntityRepository.findByUserId(username)
            .map(UserRoleEntity::getRoleId);
    }

    @Override
    public Mono<Map<String, Collection<String>>> getRolesByUsernames(Collection<String> usernames) {
        if (CollectionUtils.isEmpty(usernames)) {
            return Mono.just(Map.of());
        }
        return userRoleEntityRepository.findByUserIdIn(usernames)
            .collectMultimap(UserRoleEntity::getUserId, UserRoleEntity::getRoleId);
    }

    @Override
    public Mono<Boolean> contains(Collection<String> roleNames,
        Collection<String> permissionNames) {
        if (roleNames.contains(SuperAdminInitializer.SUPER_ROLE_NAME)) {
            return Mono.just(true);
        }
        return populatePermissionIdsByRoleIds(roleNames)
            .filter(populated -> populated.containsAll(permissionNames))
            .hasElement();
    }

    @Override
    public Flux<Role> listPermissions(Set<String> roleIds) {
        return populatePermissionsByRoleIds(roleIds)
            .buffer(20)
            .flatMap(permissions -> {
                // convert to Role
                var ids = permissions.stream()
                    .map(PermissionEntity::getId)
                    .collect(Collectors.toSet());
                return labelService.getLabels(PermissionEntity.GK, ids)
                    .map(labelsMap -> permissions.stream()
                        .map(permission -> {
                            var role = RoleExtensionAdapter.permissionToRole(permission);
                            var labels = labelsMap.get(permission.getId());
                            if (!CollectionUtils.isEmpty(labels)) {
                                if (role.getMetadata().getLabels() == null) {
                                    role.getMetadata().setLabels(new HashMap<>());
                                }
                                role.getMetadata().getLabels().putAll(labels);
                            }
                            return role;
                        })
                        .toList()
                    )
                    .flatMapIterable(Function.identity());
            });
    }

    @Override
    public Flux<Role> listDependenciesFlux(Set<String> roleIds) {
        return listPermissions(roleIds);
    }

    Predicate<RoleBinding> getRoleBindingPredicate(Subject targetSubject) {
        return roleBinding -> {
            List<Subject> subjects = roleBinding.getSubjects();
            for (Subject subject : subjects) {
                return matchSubject(targetSubject, subject);
            }
            return false;
        };
    }

    private static boolean matchSubject(Subject targetSubject, Subject subject) {
        if (targetSubject == null || subject == null) {
            return false;
        }
        return StringUtils.equals(targetSubject.getKind(), subject.getKind())
            && StringUtils.equals(targetSubject.getName(), subject.getName())
            && StringUtils.defaultString(targetSubject.getApiGroup())
            .equals(StringUtils.defaultString(subject.getApiGroup()));
    }

    @Override
    public Flux<Role> list(Set<String> roleIds) {
        return roleEntityRepository.findAllById(roleIds)
            .buffer(20)
            .flatMap(roleEntities -> {
                var ids = roleEntities.stream().map(RoleEntity::getId).collect(Collectors.toSet());
                return labelService.getLabels(RoleEntity.GK, ids)
                    .map(labelsMap -> roleEntities.stream()
                        .map(roleEntity -> {
                            var role = RoleExtensionAdapter.roleEntityToRole(roleEntity);
                            var labels = labelsMap.get(roleEntity.getId());
                            if (!CollectionUtils.isEmpty(labels)) {
                                if (role.getMetadata().getLabels() == null) {
                                    role.getMetadata().setLabels(new HashMap<>());
                                }
                                role.getMetadata().getLabels().putAll(labels);
                            }
                            return role;
                        })
                        .toList()
                    )
                    .flatMapIterable(Function.identity());
            });
    }

    @Override
    public Flux<Role> list(Set<String> roleNames, boolean excludeHidden) {
        if (CollectionUtils.isEmpty(roleNames)) {
            return Flux.empty();
        }
        var builder = ListOptions.builder()
            .andQuery(notDeleting())
            .andQuery(QueryFactory.in("metadata.name", roleNames));
        if (excludeHidden) {
            builder.labelSelector().notEq(Role.HIDDEN_LABEL_NAME, Boolean.TRUE.toString());
        }
        return client.listAll(Role.class, builder.build(), defaultSort());
    }

    private Mono<Set<String>> populatePermissionIdsByRoleIds(Collection<String> roleIds) {
        return rolePermissionEntityRepository.findByRoleIdIn(roleIds)
            .map(RolePermissionEntity::getPermissionId)
            .collect(Collectors.toSet())
            .expand(permissionIds -> permissionEntityRepository.findAllById(permissionIds)
                .mapNotNull(PermissionEntity::getDependencies)
                .filter(dependencies -> !dependencies.isEmpty())
                .flatMapIterable(set -> set)
                .collect(Collectors.toSet())
            )
            .flatMapIterable(set -> set)
            .collect(Collectors.toSet())
            .flatMap(permissionIds -> listAggregatedPermissions(permissionIds)
                .map(PermissionEntity::getId)
                .concatWith(Flux.fromIterable(permissionIds))
                .collect(Collectors.toSet())
            );
    }

    private Flux<PermissionEntity> populatePermissionsByRoleIds(Collection<String> roleIds) {
        var visited = new HashSet<String>();
        return rolePermissionEntityRepository.findByRoleIdIn(roleIds)
            .map(RolePermissionEntity::getPermissionId)
            .collect(Collectors.toSet())
            .expand(permissionIds -> permissionEntityRepository.findAllById(permissionIds)
                .mapNotNull(PermissionEntity::getDependencies)
                .flatMapIterable(Function.identity())
                .filter(permissionId -> !visited.contains(permissionId))
                .doOnNext(visited::add)
                .collect(Collectors.toSet())
                .filter(s -> !s.isEmpty())
            )
            .flatMapIterable(Function.identity())
            .collect(Collectors.toSet())
            .flatMapMany(permissionIds -> listAggregatedPermissions(permissionIds)
                .concatWith(Flux.defer(() -> permissionEntityRepository.findAllById(permissionIds)))
            )
            .distinct();
    }

    private Flux<PermissionEntity> listAggregatedPermissions(Set<String> permissionNames) {
        if (CollectionUtils.isEmpty(permissionNames)) {
            return Flux.empty();
        }
        var aggregatedLabelNames = permissionNames.stream()
            .map(permissionName -> Role.ROLE_AGGREGATE_LABEL_PREFIX + permissionName)
            .toArray(String[]::new);

        var options = ListOptions.builder()
            .labelSelector()
            .in(Role.TEMPLATE_LABEL_NAME, aggregatedLabelNames)
            .end()
            .build();
        // because we only query by label, so we use an empty map here
        return QueryUtils.findAllBy(entityTemplate, options, Sort.unsorted(), Map.of(),
            PermissionEntity.GK, PermissionEntity.class);
    }

}
