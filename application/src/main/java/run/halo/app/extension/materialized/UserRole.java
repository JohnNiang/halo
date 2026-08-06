package run.halo.app.extension.materialized;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Embedded;
import org.springframework.data.relational.core.mapping.Table;

/**
 * R2DBC entity mapping to the {@code user_roles} table with a composite primary key.
 *
 * @author halo
 * @since 2.21.0
 */
@Data
@Table(name = "user_roles")
public class UserRole {

    @Id
    @Embedded(onEmpty = Embedded.OnEmpty.USE_NULL)
    private UserRoleId id;
}
