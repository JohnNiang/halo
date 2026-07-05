package run.halo.app.extension.materialized;

import reactor.core.publisher.Mono;

/**
 * Custom repository operations for {@link ExtensionLabel} that work around the limitation
 * of {@code R2dbcRepository.save()} with {@code @Embedded} composite primary keys.
 *
 * @author halo
 * @since 2.21.0
 */
public interface ExtensionLabelRepositoryCustom {

    /**
     * Inserts an {@link ExtensionLabel} into the database. Unlike the default
     * {@code save()} provided by {@code R2dbcRepository}, this method always performs an
     * INSERT, which is required for entities with {@code @Embedded} composite primary keys.
     */
    Mono<ExtensionLabel> insert(ExtensionLabel entity);
}
