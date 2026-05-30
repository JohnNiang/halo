package run.halo.app.notification;

import java.time.Instant;
import java.util.Map;

/**
 * Notification domain object representing a single on-site notification for a user.
 * This is a POJO, not an extension CRD.
 *
 * @author johnniang
 * @since 2.20.0
 */
public class Notification {

    private Long id;

    private String recipient;

    private String category;

    private String messageKey;

    private Map<String, Object> messageArgs;

    private String subjectUrl;

    private boolean unread = true;

    private Instant createdAt;

    private Instant readAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRecipient() { return recipient; }
    public void setRecipient(String recipient) { this.recipient = recipient; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getMessageKey() { return messageKey; }
    public void setMessageKey(String messageKey) { this.messageKey = messageKey; }

    public Map<String, Object> getMessageArgs() { return messageArgs; }
    public void setMessageArgs(Map<String, Object> messageArgs) { this.messageArgs = messageArgs; }

    public String getSubjectUrl() { return subjectUrl; }
    public void setSubjectUrl(String subjectUrl) { this.subjectUrl = subjectUrl; }

    public boolean isUnread() { return unread; }
    public void setUnread(boolean unread) { this.unread = unread; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getReadAt() { return readAt; }
    public void setReadAt(Instant readAt) { this.readAt = readAt; }
}
