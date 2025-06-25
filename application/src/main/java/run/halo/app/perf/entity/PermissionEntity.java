package run.halo.app.perf.entity;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;
import run.halo.app.core.extension.Role;
import run.halo.app.extension.GroupKind;

@Data
@Table("permissions")
public class PermissionEntity implements Persistable<String> {

    public static final GroupKind GK = new GroupKind("", "Permission");

    @Transient
    private boolean isNew;

    @Id
    private String id;

    @Version
    private Long version;

    private String displayName;

    private String description;

    private String category;

    private boolean hidden;

    private Set<String> uiPermissions;

    private Set<Role.PolicyRule> rules;

    private Set<String> dependencies;

    private Set<String> finalizers;

    private Map<String, String> annotations;

    @CreatedDate
    private Instant createdDate;

    private Instant deletedDate;

    /**
     * Mark this entity as new.
     */
    public void markAsNew() {
        this.isNew = true;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }
}
