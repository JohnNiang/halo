package run.halo.app.notification;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Email notifier that sends notifications via email.
 *
 * @author guqing
 * @since 2.10.0
 */
@Component
public class EmailNotifier implements ReactiveNotifier {

    @Override
    public String name() {
        return "email-notifier";
    }

    @Override
    public Mono<Void> notify(Notification notification) {
        // TODO: Implement in Group 7
        return Mono.empty();
    }

    @Override
    public boolean supports(String recipient) {
        // TODO: Implement in Group 7
        return false;
    }
}
