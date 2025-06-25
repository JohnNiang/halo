package run.halo.app.perf.entity;

import java.time.Instant;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Table("user_roles")
public class UserRoleEntity {

    @Id
    private Long id;

    private String userId;

    private String roleId;

    @CreatedDate
    private Instant createdDate;

    @Version
    private Long version;

}
