package run.halo.app.notification;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Core service for persisting and managing on-site notifications.
 *
 * @author johnniang
 * @since 2.20.0
 */
public interface NotificationService {

    /** Persists one notification per recipient. Category is validated against the registry. */
    Mono<Void> notify(NotificationRequest request);

    /** Lists notifications for a user, optionally filtered by unread status. */
    Flux<Notification> listByUser(String username, Boolean unread, int offset, int limit);

    /** Counts unread notifications for a user. */
    Mono<Long> countUnread(String username);

    /** Marks a single notification as read. Only the recipient may do this. */
    Mono<Void> markAsRead(String username, Long notificationId);

    /** Marks multiple notifications as read in batch. */
    Mono<Void> markAsRead(String username, Iterable<Long> notificationIds);

    /** Deletes a single notification owned by the user. */
    Mono<Void> delete(String username, Long notificationId);

    /** Deletes multiple notifications in batch. */
    Mono<Void> delete(String username, Iterable<Long> notificationIds);
}
