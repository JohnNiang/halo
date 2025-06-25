package run.halo.app.perf.migration;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.Role;
import run.halo.app.core.extension.RoleBinding;
import run.halo.app.core.extension.User;
import run.halo.app.extension.ExtensionConverter;
import run.halo.app.extension.ExtensionStoreUtil;
import run.halo.app.extension.Scheme;
import run.halo.app.extension.store.ReactiveExtensionStoreClient;
import run.halo.app.perf.entity.UserRoleEntity;
import run.halo.app.perf.repository.UserRoleEntityRepository;

@Slf4j
@Component
class RoleBindingMigration implements ExtensionMigration {

    private final ReactiveExtensionStoreClient storeClient;

    private final UserRoleEntityRepository userRoleEntityRepository;

    private final ExtensionConverter converter;

    private final ReactiveTransactionManager txManager;

    RoleBindingMigration(ReactiveExtensionStoreClient storeClient,
        UserRoleEntityRepository userRoleEntityRepository,
        ExtensionConverter converter,
        ReactiveTransactionManager txManager) {
        this.storeClient = storeClient;
        this.userRoleEntityRepository = userRoleEntityRepository;
        this.converter = converter;
        this.txManager = txManager;
    }

    @Override
    public boolean support(Scheme scheme) {
        return RoleBinding.class == scheme.type();
    }

    @Override
    public Mono<Void> migrate(Scheme scheme) {
        // detect if role bindings have been migrated
        return userRoleEntityRepository.count()
            .filter(count -> count == 0)
            .switchIfEmpty(Mono.fromRunnable(
                    () -> log.info("Role bindings have been migrated, skip migration.")
                ).then(Mono.empty())
            )
            .flatMap(ignored -> {
                var prefix = ExtensionStoreUtil.buildStoreNamePrefix(scheme);
                var tx = TransactionalOperator.create(txManager);
                return storeClient.listByNamePrefix(prefix)
                    .limitRate(100)
                    .map(store -> converter.convertFrom(RoleBinding.class, store))
                    .flatMapIterable(rb -> {
                        var subjects = rb.getSubjects();
                        if (subjects == null || subjects.isEmpty()) {
                            return Set.of();
                        }
                        var roleRef = rb.getRoleRef();
                        if (roleRef == null || !Objects.equals(Role.KIND, roleRef.getKind())) {
                            return Set.of();
                        }
                        var roleName = roleRef.getName();
                        return subjects.stream()
                            .filter(subject -> Objects.equals(User.KIND, subject.getKind()))
                            .map(subject -> {
                                // build user role entity
                                var entity = new UserRoleEntity();
                                entity.setUserId(subject.getName());
                                entity.setRoleId(roleName);
                                return entity;
                            })
                            .collect(Collectors.toSet());
                    })
                    .buffer(100)
                    .flatMap(userRoleEntityRepository::saveAll)
                    .as(tx::transactional)
                    .then();
            });
    }

}
