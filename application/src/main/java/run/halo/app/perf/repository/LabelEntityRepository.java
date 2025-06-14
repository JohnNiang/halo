package run.halo.app.perf.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import run.halo.app.perf.entity.LabelEntity;

public interface LabelEntityRepository extends R2dbcRepository<LabelEntity, Long> {

    Flux<LabelEntity> findByEntityTypeAndEntityId(String entityType, String entityId);

}
