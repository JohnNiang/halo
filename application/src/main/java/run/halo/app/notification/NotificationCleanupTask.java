package run.halo.app.notification;

import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled task to delete read notifications older than the configured retention period.
 *
 * @author johnniang
 * @since 2.20.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationCleanupTask {

    private final R2dbcEntityTemplate r2dbcTemplate;
    private final NotificationCleanupProperties properties;

    @Scheduled(cron = "${halo.notification.cleanup.cron:0 0 3 * * ?}")
    public void cleanup() {
        if (!properties.isEnabled()) {
            return;
        }
        var cutoff = Instant.now().minus(Duration.ofDays(properties.getRetentionDays()));
        r2dbcTemplate.getDatabaseClient()
                .sql("DELETE FROM notifications WHERE is_unread = false AND created_at < :cutoff")
                .bind("cutoff", cutoff)
                .fetch()
                .rowsUpdated()
                .doOnNext(deleted -> log.info("Cleaned up {} old read notifications", deleted))
                .subscribe();
    }
}
