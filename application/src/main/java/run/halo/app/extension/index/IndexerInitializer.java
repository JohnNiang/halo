package run.halo.app.extension.index;

import java.time.Duration;
import java.util.Objects;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.User;
import run.halo.app.extension.Extension;
import run.halo.app.extension.ExtensionConverter;
import run.halo.app.extension.ExtensionStoreUtil;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.ListResult;
import run.halo.app.extension.PageRequestImpl;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.extension.Scheme;
import run.halo.app.extension.event.IndexerBuiltEvent;
import run.halo.app.extension.event.SchemeAddedEvent;
import run.halo.app.extension.event.SchemeRemovedEvent;
import run.halo.app.extension.store.ReactiveExtensionStoreClient;

/**
 * Build indexer for each scheme. When a scheme is added, an indexer will be created for it.
 *
 * @author johnniang
 */
@Slf4j
@Component
class IndexerInitializer {

    private final ReactiveExtensionStoreClient storeClient;

    private final ExtensionConverter converter;

    private final IndexedQueryEngine indexedQueryEngine;

    private final IndexerFactory indexerFactory;

    private final ApplicationEventPublisher eventPublisher;

    private final ReactiveExtensionClient client;

    IndexerInitializer(ReactiveExtensionStoreClient storeClient,
        ExtensionConverter converter,
        IndexedQueryEngine indexedQueryEngine,
        IndexerFactory indexerFactory,
        ApplicationEventPublisher eventPublisher,
        ReactiveExtensionClient client
    ) {
        this.storeClient = storeClient;
        this.converter = converter;
        this.indexedQueryEngine = indexedQueryEngine;
        this.indexerFactory = indexerFactory;
        this.eventPublisher = eventPublisher;
        this.client = client;
    }


    @Order(Ordered.HIGHEST_PRECEDENCE)
    @EventListener
    void handleSchemeAddedEvent(SchemeAddedEvent event) {
        createIndexerFor(event.getScheme());
    }

    @Order
    @EventListener
    void handleSchemeRemovedEvent(SchemeRemovedEvent event) {
        indexerFactory.removeIndexer(event.getScheme());
    }

    private void createIndexerFor(Scheme scheme) {
        // skip for user
        if (Objects.equals(User.class, scheme.type())) {
            // TODO Check if the user entity is empty
            // migrate it
            var prefix = ExtensionStoreUtil.buildStoreNamePrefix(scheme);
            client.listBy(User.class, ListOptions.builder().build(), PageRequestImpl.of(1, 1))
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
                .flatMap(zero -> storeClient.listByNamePrefix(prefix)
                    .limitRate(100)
                    .flatMap(store -> {
                        var user = converter.convertFrom(User.class, store);
                        return client.create(user).doOnSuccess(deleted ->
                            log.info("Migrated user: {} to new store",
                                deleted.getMetadata().getName())
                        );
                    })
                    .then())
                .block(Duration.ofMinutes(1));
            return;
        }

        var iterator = createExtensionIterator(scheme);
        indexerFactory.createIndexerFor(scheme.type(), iterator);
        // ensure data count matches index count
        var prefix = ExtensionStoreUtil.buildStoreNamePrefix(scheme);
        var indexedSize = indexedQueryEngine.retrieveAll(scheme.groupVersionKind(),
            new ListOptions(), Sort.unsorted()).size();
        long count = storeClient.countByNamePrefix(prefix).blockOptional().orElseThrow();
        if (count != iterator.size() || count != indexedSize) {
            log.error("indexedSize: {}, count in db: {}, iterate size: {}", indexedSize, count,
                iterator.size());
            throw new IllegalStateException("The number of indexed records is not equal to the "
                + "number of records in the database, this is a serious error, please submit "
                + "an issue to halo.");
        }
        eventPublisher.publishEvent(new IndexerBuiltEvent(this, scheme));
    }

    @NonNull
    private ExtensionIterator<Extension> createExtensionIterator(Scheme scheme) {
        var type = scheme.type();
        var prefix = ExtensionStoreUtil.buildStoreNamePrefix(scheme);
        return new DefaultExtensionIterator<>(pageable ->
            storeClient.listByNamePrefix(prefix, pageable)
                .map(page ->
                    page.map(store -> (Extension) converter.convertFrom(type, store))
                )
                .block()
        );
    }
}
