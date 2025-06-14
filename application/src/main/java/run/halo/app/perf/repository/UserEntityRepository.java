package run.halo.app.perf.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import run.halo.app.perf.entity.UserEntity;

public interface UserEntityRepository extends R2dbcRepository<UserEntity, String> {

}
