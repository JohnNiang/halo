package run.halo.app.extension.materialized;

import java.util.Collection;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * R2DBC repository for the {@code users} table.
 *
 * @author halo
 * @since 2.21.0
 */
@Repository
public interface UserRepository extends R2dbcRepository<UserPo, String> {

    Mono<UserPo> findByEmail(String email);

    Flux<UserPo> findAllByEmailVerified(boolean emailVerified);

    Flux<UserPo> findAllByDisabled(boolean disabled);

    Flux<UserPo> findAllByNameIn(Collection<String> names);
}
