package run.halo.app.perf.repository;

import java.util.Collection;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import run.halo.app.perf.entity.UserRoleEntity;

public interface UserRoleEntityRepository extends R2dbcRepository<UserRoleEntity, Long> {

    Flux<UserRoleEntity> findByUserId(String userId);

    Flux<UserRoleEntity> findByUserIdIn(Collection<String> usernames);

}
