package run.halo.app.perf.migration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.User;
import run.halo.app.extension.ExtensionConverter;
import run.halo.app.extension.ExtensionStoreUtil;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.ListResult;
import run.halo.app.extension.PageRequestImpl;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.extension.Scheme;
import run.halo.app.extension.store.ReactiveExtensionStoreClient;

@Slf4j
@Component
class UserMigration implements ExtensionMigration {

    private final ReactiveExtensionClient client;

    private final ReactiveExtensionStoreClient storeClient;

    private final ExtensionConverter converter;

    UserMigration(ReactiveExtensionClient client,
        ReactiveExtensionStoreClient storeClient,
        ExtensionConverter converter) {
        this.client = client;
        this.storeClient = storeClient;
        this.converter = converter;
    }

    @Override
    public boolean support(Scheme scheme) {
        return User.class == scheme.type();
    }

    @Override
    public Mono<Void> migrate(Scheme scheme) {
        return client.listBy(User.class, ListOptions.builder().build(), PageRequestImpl.of(1, 1))
            .map(ListResult::getTotal)
            .filter(total -> total == 0)
            .switchIfEmpty(
                Mono.fromRunnable(() -> log.info(
                        "Already migrated users to new store, skip migration for scheme: "
                            + "{}",
                        scheme.groupVersionKind()
                    ))
                    .then(Mono.empty())
            )
            .flatMap(zero -> {
                var prefix = ExtensionStoreUtil.buildStoreNamePrefix(scheme);
                return storeClient.listByNamePrefix(prefix)
                    .limitRate(100)
                    .flatMap(store -> {
                        var user = converter.convertFrom(User.class, store);
                        return client.create(user).doOnSuccess(created ->
                            log.info("Migrated user: {}", created.getMetadata().getName())
                        );
                    })
                    .then();
            });
    }
}
