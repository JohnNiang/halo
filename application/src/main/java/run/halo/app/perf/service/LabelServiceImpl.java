package run.halo.app.perf.service;

import java.util.Collection;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Mono;
import run.halo.app.extension.GroupKind;
import run.halo.app.perf.entity.LabelEntity;
import run.halo.app.perf.repository.LabelEntityRepository;

@Service
class LabelServiceImpl implements LabelService {

    private final LabelEntityRepository labelEntityRepository;

    public LabelServiceImpl(LabelEntityRepository labelEntityRepository) {
        this.labelEntityRepository = labelEntityRepository;
    }

    @Override
    public Mono<Void> saveLabels(GroupKind groupKind, String entityId, Map<String, String> labels) {
        var entityType = groupKind.toString();
        return labelEntityRepository.findByEntityTypeAndEntityId(entityType, entityId)
            .collectList()
            .flatMap(labelEntityRepository::deleteAll)
            .then(Mono.fromSupplier(
                () -> {
                    if (labels == null || labels.isEmpty()) {
                        // delete all labels only when labels is null or empty
                        return null;
                    }
                    return labels.keySet()
                        .stream()
                        .map(labelKey -> {
                            var labelValue = labels.get(labelKey);
                            var labelEntity = new LabelEntity();
                            labelEntity.setLabelName(labelKey);
                            labelEntity.setLabelValue(labelValue);
                            labelEntity.setEntityType(entityType);
                            labelEntity.setEntityId(entityId);
                            return labelEntity;
                        })
                        .toList();
                }
            ))
            .flatMapMany(labelEntityRepository::saveAll)
            .then();
    }

    @Override
    public Mono<Map<String, Map<String, String>>> getLabels(GroupKind groupKind,
        Collection<String> entityIds) {
        if (CollectionUtils.isEmpty(entityIds)) {
            return Mono.just(Map.of());
        }
        var entityType = groupKind.toString();
        return labelEntityRepository.findByEntityTypeAndEntityIdIn(entityType, entityIds)
            .collectMap(
                LabelEntity::getEntityId,
                labelEntity -> Map.of(labelEntity.getLabelName(), labelEntity.getLabelValue())
            );
    }

    @Override
    public Mono<Map<String, String>> getLabels(GroupKind groupKind, String entityId) {
        return labelEntityRepository.findByEntityTypeAndEntityId(groupKind.toString(), entityId)
            .collectMap(LabelEntity::getLabelName, LabelEntity::getLabelValue)
            .defaultIfEmpty(Map.of());
    }

}
