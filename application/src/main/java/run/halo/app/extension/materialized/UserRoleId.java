package run.halo.app.extension.materialized;

import lombok.Data;
import org.springframework.data.relational.core.mapping.Column;

/**
 * Composite primary key for {@link UserRole}.
 *
 * @author halo
 * @since 2.21.0
 */
@Data
public class UserRoleId {

    @Column("store_name")
    private String storeName;

    @Column("role_name")
    private String roleName;
}
