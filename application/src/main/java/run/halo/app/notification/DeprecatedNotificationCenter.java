package run.halo.app.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.notification.Reason;
import run.halo.app.core.extension.notification.Subscription;

/**
 * No-op implementation of {@link NotificationCenter} for backward compatibility with plugins compiled against the old
 * notification API. All methods log a deprecation warning and return {@link Mono#empty()}.
 *
 * @author johnniang
 * @since 2.20.0
 */
@Slf4j
@Component
public class DeprecatedNotificationCenter implements NotificationCenter {

    private static final String DEPRECATION_MSG =
            "NotificationCenter is deprecated since 2.20.0. Use NotificationService instead. "
                    + "Calls to this interface are no-ops.";

    public DeprecatedNotificationCenter() {
        log.warn(DEPRECATION_MSG);
    }

    @Override
    public Mono<Void> notify(Reason reason) {
        return Mono.empty();
    }

    @Override
    public Mono<Subscription> subscribe(Subscription.Subscriber subscriber, Subscription.InterestReason reason) {
        return Mono.empty();
    }

    @Override
    public Mono<Void> unsubscribe(Subscription.Subscriber subscriber) {
        return Mono.empty();
    }

    @Override
    public Mono<Void> unsubscribe(Subscription.Subscriber subscriber, Subscription.InterestReason reason) {
        return Mono.empty();
    }
}
