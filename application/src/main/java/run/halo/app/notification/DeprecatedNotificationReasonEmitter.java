package run.halo.app.notification;

import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * No-op implementation of {@link NotificationReasonEmitter} for backward compatibility with plugins compiled against
 * the old notification API. All methods log a deprecation warning and return {@link Mono#empty()}.
 *
 * @author johnniang
 * @since 2.20.0
 */
@Slf4j
@Component
public class DeprecatedNotificationReasonEmitter implements NotificationReasonEmitter {

    public DeprecatedNotificationReasonEmitter() {
        log.warn("NotificationReasonEmitter is deprecated since 2.20.0. "
                + "Use NotificationService instead. Calls to this interface are no-ops.");
    }

    @Override
    public Mono<Void> emit(String reasonType, Consumer<ReasonPayload.ReasonPayloadBuilder> reasonData) {
        return Mono.empty();
    }
}
