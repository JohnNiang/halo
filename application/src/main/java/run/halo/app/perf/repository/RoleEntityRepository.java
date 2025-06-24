package run.halo.app.perf.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import run.halo.app.perf.entity.RoleEntity;

public interface RoleEntityRepository extends R2dbcRepository<RoleEntity, String> {

}
