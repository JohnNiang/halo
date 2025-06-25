package run.halo.app.perf.repository;

import java.util.Collection;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import run.halo.app.perf.entity.RolePermissionEntity;

public interface RolePermissionEntityRepository
    extends R2dbcRepository<RolePermissionEntity, Long> {

    Flux<RolePermissionEntity> findByRoleIdIn(Collection<String> roleNames);

}
