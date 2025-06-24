package run.halo.app.perf.service;

import java.util.Collection;
import java.util.Map;
import reactor.core.publisher.Mono;
import run.halo.app.extension.GroupKind;

public interface LabelService {

    Mono<Void> saveLabels(GroupKind groupKind, String entityId, Map<String, String> labels);

    Mono<Map<String, Map<String, String>>> getLabels(GroupKind groupKind,
        Collection<String> entityIds);

    Mono<Map<String, String>> getLabels(GroupKind groupKind, String entityId);

}
