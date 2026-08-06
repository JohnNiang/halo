package run.halo.app.extension.materialized;

import java.time.Instant;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;

/**
 * R2DBC entity mapping to the {@code users} table.
 *
 * @author halo
 * @since 2.21.0
 */
@Data
@Table(name = "users")
public class UserPo {

    @Id
    private String name;

    private String displayName;

    private String email;

    private boolean emailVerified;

    private boolean disabled;

    private Instant creationTimestamp;

    private Instant deletionTimestamp;

    @Version
    private Long version;
}
