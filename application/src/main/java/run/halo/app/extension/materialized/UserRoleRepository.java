package run.halo.app.extension.materialized;

import java.util.Collection;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * R2DBC repository for the {@code user_roles} table.
 *
 * @author halo
 * @since 2.21.0
 */
@Repository
public interface UserRoleRepository extends R2dbcRepository<UserRole, UserRoleId>,
    UserRoleRepositoryCustom {

    @Query("SELECT store_name FROM user_roles WHERE role_name = :roleName")
    Flux<String> findStoreNamesByRoleName(@Param("roleName") String roleName);

    @Query("SELECT store_name FROM user_roles WHERE role_name IN (:roleNames)")
    Flux<String> findStoreNamesByRoleNameIn(
        @Param("roleNames") Collection<String> roleNames
    );

    @Query("DELETE FROM user_roles WHERE store_name = :storeName")
    Mono<Void> deleteByStoreName(@Param("storeName") String storeName);
}
