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

/**
 * User entity representing a user in the system.
 *
 * @author johnniang
 */
@Data
@Table("users")
public class UserEntity implements Auditable<String, String, Instant> {

    @Transient
    private boolean isNew;

    @Id
    private String id;

    @Version
    private Long version;

    private String displayName;

    private String avatar;

    private String bio;

    private String email;

    private boolean emailVerified;

    private String phone;

    private String encodedPassword;

    private Instant deletedDate;

    private boolean twoFactorAuthEnabled;

    private String totpEncryptedSecret;

    private boolean disabled;

    private Map<String, String> annotations;

    private Set<String> finalizers;

    private Instant createdDate;

    private String createdBy;

    private Instant lastModifiedDate;

    private String lastModifiedBy;

    /**
     * Marks this entity as new. This is used to indicate that the entity has not been persisted
     * yet.
     */
    public void markAsNew() {
        this.isNew = true;
    }

    @Override
    public boolean isNew() {
        return this.isNew;
    }

    @Override
    public Optional<Instant> getCreatedDate() {
        return Optional.ofNullable(createdDate);
    }

    @Override
    public Optional<Instant> getLastModifiedDate() {
        return Optional.ofNullable(lastModifiedDate);
    }

    @Override
    public Optional<String> getCreatedBy() {
        return createdBy.describeConstable();
    }

    @Override
    public Optional<String> getLastModifiedBy() {
        return lastModifiedBy.describeConstable();
    }
}
