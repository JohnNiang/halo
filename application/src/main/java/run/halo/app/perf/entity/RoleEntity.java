package run.halo.app.perf.entity;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.Auditable;
import org.springframework.data.relational.core.mapping.Table;
import run.halo.app.extension.GroupKind;

@Data
@Table("roles")
public class RoleEntity implements Auditable<String, String, Instant> {

    public static final GroupKind GK = new GroupKind("", "Role");

    @Transient
    private boolean isNew;

    @Id
    private String id;

    @Version
    private Long version;

    private String displayName;

    private String description;

    private boolean reserved;

    private boolean hidden;

    private Instant deletedDate;

    private Instant createdDate;

    private String createdBy;

    private Instant lastModifiedDate;

    private String lastModifiedBy;

    private Set<String> finalizers;

    private Map<String, String> annotations;

    @Override
    public Optional<String> getCreatedBy() {
        return Optional.ofNullable(createdBy);
    }

    @Override
    public Optional<Instant> getCreatedDate() {
        return Optional.ofNullable(createdDate);
    }

    @Override
    public Optional<String> getLastModifiedBy() {
        return Optional.ofNullable(lastModifiedBy);
    }

    @Override
    public Optional<Instant> getLastModifiedDate() {
        return Optional.ofNullable(lastModifiedDate);
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    /**
     * Mark this entity as new.
     */
    public void markAsNew() {
        this.isNew = true;
    }
}
