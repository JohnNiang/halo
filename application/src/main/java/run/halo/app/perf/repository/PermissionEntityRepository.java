package run.halo.app.perf.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import run.halo.app.perf.entity.PermissionEntity;

public interface PermissionEntityRepository extends R2dbcRepository<PermissionEntity, String> {

}
