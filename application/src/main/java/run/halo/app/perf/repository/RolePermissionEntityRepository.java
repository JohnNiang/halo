package run.halo.app.perf.repository;

import java.util.Collection;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.perf.entity.RolePermissionEntity;

public interface RolePermissionEntityRepository
    extends R2dbcRepository<RolePermissionEntity, Long> {

    Flux<RolePermissionEntity> findByRoleIdIn(Collection<String> roleIds);

    Flux<RolePermissionEntity> findByRoleId(String roleId);

    Mono<Void> deleteByRoleIdAndPermissionIdIn(String roleId, Collection<String> permissionIds);

}
