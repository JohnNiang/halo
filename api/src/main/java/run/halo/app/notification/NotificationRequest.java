package run.halo.app.notification;

import java.util.Map;
import java.util.Set;

/**
 * Request to send a notification. The caller resolves recipients in its own domain logic.
 *
 * @param recipients usernames to notify
 * @param category notification category key from the registry
 * @param messageKey i18n key for the notification message
 * @param messageArgs structured arguments for i18n rendering
 * @param subjectUrl link back to the source entity (optional)
 * @author johnniang
 * @since 2.20.0
 */
public record NotificationRequest(
        Set<String> recipients,
        String category,
        String messageKey,
        Map<String, Object> messageArgs,
        String subjectUrl) {}
