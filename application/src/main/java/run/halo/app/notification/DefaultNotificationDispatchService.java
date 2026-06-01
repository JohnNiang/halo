package run.halo.app.notification;

import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import reactor.core.publisher.Mono;
import run.halo.app.plugin.extensionpoint.ExtensionGetter;

/**
 * Dispatches notifications to notifiers after the notification transaction commits.
 *
 * @author johnniang
 * @since 2.20.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultNotificationDispatchService {

    private static final int MAX_RETRIES = 3;
    private static final Duration INITIAL_BACKOFF = Duration.ofSeconds(30);

    private final R2dbcEntityTemplate r2dbcTemplate;
    private final ExtensionGetter extensionGetter;
    private final NotificationPreferenceService preferenceService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onNotificationPersisted(NotificationPersistedEvent event) {
        dispatch(event.getNotification()).subscribe();
    }

    Mono<Void> dispatch(Notification notification) {
        return preferenceService
                .getEnabledNotifiers(notification.getRecipient(), notification.getCategory())
                .defaultIfEmpty(java.util.Set.of())
                .flatMapMany(enabledNotifiers -> {
                    var notifiers = extensionGetter.getExtensionList(ReactiveNotifier.class);
                    return reactor.core.publisher.Flux.fromIterable(notifiers)
                            .filter(n -> enabledNotifiers.isEmpty() || enabledNotifiers.contains(n.name()))
                            .filter(n -> n.supports(notification.getRecipient()));
                })
                .flatMap(notifier -> {
                    var dispatchId = createDispatch(notification, notifier.name());
                    return dispatchId.flatMap(id -> notifier.notify(notification)
                            .then(markDispatchSent(id))
                            .onErrorResume(e -> markDispatchFailed(id, e.getMessage())));
                })
                .then();
    }

    private Mono<Long> createDispatch(Notification notification, String notifierName) {
        return r2dbcTemplate
                .getDatabaseClient()
                .sql("""
                        INSERT INTO notification_dispatches (notification_id, notifier, status, created_at)
                        VALUES (:notificationId, :notifier, 'PENDING', :now)
                        """)
                .bind("notificationId", notification.getId())
                .bind("notifier", notifierName)
                .bind("now", Instant.now())
                .filter((statement, executeFunction) ->
                        statement.returnGeneratedValues("id").execute())
                .map(row -> row.get("id", Long.class))
                .one();
    }

    private Mono<Void> markDispatchSent(Long dispatchId) {
        return r2dbcTemplate
                .getDatabaseClient()
                .sql("UPDATE notification_dispatches SET status = 'SENT', completed_at = :now WHERE id = :id")
                .bind("id", dispatchId)
                .bind("now", Instant.now())
                .then();
    }

    private Mono<Void> markDispatchFailed(Long dispatchId, String errorMessage) {
        return r2dbcTemplate
                .getDatabaseClient()
                .sql("""
                        UPDATE notification_dispatches
                        SET status = 'FAILED', error_message = :error, completed_at = :now, retry_count = retry_count + 1,
                            next_retry_at = :nextRetry
                        WHERE id = :id
                        """)
                .bind("id", dispatchId)
                .bind("error", errorMessage)
                .bind("now", Instant.now())
                .bind("nextRetry", Instant.now().plus(INITIAL_BACKOFF))
                .then()
                .then(retryFailedDispatch(dispatchId));
    }

    private Mono<Void> retryFailedDispatch(Long dispatchId) {
        return r2dbcTemplate
                .getDatabaseClient()
                .sql("SELECT retry_count FROM notification_dispatches WHERE id = :id")
                .bind("id", dispatchId)
                .map(row -> row.get("retry_count", Integer.class))
                .one()
                .flatMap(retryCount -> {
                    if (retryCount >= MAX_RETRIES) {
                        return Mono.empty();
                    }
                    var delay = INITIAL_BACKOFF.multipliedBy(retryCount + 1);
                    return Mono.delay(delay)
                            .then(r2dbcTemplate
                                    .getDatabaseClient()
                                    .sql("SELECT notification_id, notifier FROM notification_dispatches WHERE id = :id")
                                    .bind("id", dispatchId)
                                    .map(row -> {
                                        var n = new Notification();
                                        n.setId(row.get("notification_id", Long.class));
                                        return n;
                                    })
                                    .one())
                            .flatMap(notification -> {
                                var notifiers = extensionGetter.getExtensionList(ReactiveNotifier.class);
                                return reactor.core.publisher.Flux.fromIterable(notifiers)
                                        .filter(n -> n.supports(notification.getRecipient()))
                                        .next()
                                        .flatMap(n -> n.notify(notification)
                                                .then(markDispatchSent(dispatchId))
                                                .onErrorResume(e -> markDispatchFailed(dispatchId, e.getMessage())));
                            });
                });
    }
}
