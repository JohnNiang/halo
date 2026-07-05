package run.halo.app.extension.materialized;

import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import reactor.core.publisher.Mono;

/**
 * Implementation of custom {@link UserRoleRepository} operations.
 *
 * @author halo
 * @since 2.21.0
 */
public class UserRoleRepositoryImpl implements UserRoleRepositoryCustom {

    private final R2dbcEntityTemplate template;

    public UserRoleRepositoryImpl(R2dbcEntityTemplate template) {
        this.template = template;
    }

    @Override
    public Mono<UserRole> insert(UserRole entity) {
        return template.insert(UserRole.class).using(entity);
    }
}
