package run.halo.app.extension.materialized;

import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import reactor.core.publisher.Mono;

/**
 * Implementation of custom {@link ExtensionLabelRepository} operations.
 *
 * @author halo
 * @since 2.21.0
 */
public class ExtensionLabelRepositoryImpl implements ExtensionLabelRepositoryCustom {

    private final R2dbcEntityTemplate template;

    public ExtensionLabelRepositoryImpl(R2dbcEntityTemplate template) {
        this.template = template;
    }

    @Override
    public Mono<ExtensionLabel> insert(ExtensionLabel entity) {
        return template.insert(ExtensionLabel.class).using(entity);
    }
}
