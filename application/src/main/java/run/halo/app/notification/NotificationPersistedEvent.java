package run.halo.app.notification;

import org.springframework.context.ApplicationEvent;

/**
 * Published after a notification is persisted to the database.
 * Consumed by the dispatch service via {@code @TransactionalEventListener}.
 */
public class NotificationPersistedEvent extends ApplicationEvent {

    private final Notification notification;

    public NotificationPersistedEvent(Notification notification) {
        super(notification);
        this.notification = notification;
    }

    public Notification getNotification() {
        return notification;
    }
}
