package run.halo.app.notification;

import org.pf4j.ExtensionPoint;
import reactor.core.publisher.Mono;

/**
 * Notifier extension point for delivering notifications through a channel (email, SMS, etc.). Each notifier owns its
 * own content rendering.
 *
 * @author guqing
 * @since 2.20.0
 */
public interface ReactiveNotifier extends ExtensionPoint {

    /** Returns the unique name of this notifier (e.g., "email-notifier", "sms-notifier"). */
    String name();

    /**
     * Delivers the notification through this notifier's channel. The notifier is responsible for rendering content from
     * the notification's {@code messageKey} and {@code messageArgs}.
     */
    Mono<Void> notify(Notification notification);

    /**
     * Returns whether this notifier supports the given recipient. For example, an email notifier checks if the user has
     * a verified email address.
     */
    Mono<Boolean> supports(String recipient);
}
