package run.halo.app.extension.materialized;

import reactor.core.publisher.Mono;

/**
 * Custom repository operations for {@link UserRole} that work around the limitation
 * of {@code R2dbcRepository.save()} with {@code @Embedded} composite primary keys and
 * entities with no non-ID columns.
 *
 * @author halo
 * @since 2.21.0
 */
public interface UserRoleRepositoryCustom {

    /**
     * Inserts a {@link UserRole} into the database. Unlike the default
     * {@code save()} provided by {@code R2dbcRepository}, this method always performs an
     * INSERT, which is required for entities with {@code @Embedded} composite primary keys.
     */
    Mono<UserRole> insert(UserRole entity);
}
