package run.halo.app.notification;

import java.time.Instant;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.infra.utils.JsonUtils;

/**
 * Default implementation of {@link NotificationService} using R2DBC.
 *
 * @author johnniang
 * @since 2.20.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultNotificationService implements NotificationService {

    private final R2dbcEntityTemplate r2dbcTemplate;
    private final NotificationCategoryRegistry categoryRegistry;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public Mono<Void> notify(NotificationRequest request) {
        return categoryRegistry.exists(request.category())
                .flatMap(exists -> {
                    if (!exists) {
                        return Mono.error(new IllegalArgumentException(
                                "Unknown notification category: " + request.category()));
                    }
                    return Flux.fromIterable(request.recipients())
                            .flatMap(recipient -> insertNotification(request, recipient))
                            .doOnNext(eventPublisher::publishEvent)
                            .then();
                });
    }

    private Mono<NotificationPersistedEvent> insertNotification(NotificationRequest request, String recipient) {
        var messageArgsJson = request.messageArgs() != null
                ? JsonUtils.objectToJson(request.messageArgs())
                : "{}";
        var spec = r2dbcTemplate.getDatabaseClient().sql("""
                        INSERT INTO notifications (recipient, category, message_key, message_args, subject_url, created_at)
                        VALUES (:recipient, :category, :messageKey, :messageArgs, :subjectUrl, :createdAt)
                        """)
                .bind("recipient", recipient)
                .bind("category", request.category())
                .bind("messageKey", request.messageKey())
                .bind("messageArgs", messageArgsJson);
        if (request.subjectUrl() != null) {
            spec = spec.bind("subjectUrl", request.subjectUrl());
        } else {
            spec = spec.bindNull("subjectUrl", String.class);
        }
        var now = Instant.now();
        spec = spec.bind("createdAt", now);
        return spec.filter((statement, executeFunction) -> statement.returnGeneratedValues("id").execute())
                .map(row -> {
                    var notification = new Notification();
                    notification.setId(row.get("id", Long.class));
                    notification.setRecipient(recipient);
                    notification.setCategory(request.category());
                    notification.setMessageKey(request.messageKey());
                    notification.setMessageArgs(request.messageArgs());
                    notification.setSubjectUrl(request.subjectUrl());
                    notification.setUnread(true);
                    notification.setCreatedAt(now);
                    return new NotificationPersistedEvent(notification);
                })
                .one();
    }

    @Override
    public Flux<Notification> listByUser(String username, Boolean unread, int offset, int limit) {
        var sql = new StringBuilder("""
                SELECT id, recipient, category, message_key, message_args, subject_url, is_unread, created_at, read_at
                FROM notifications WHERE recipient = :recipient
                """);
        if (unread != null) {
            sql.append(" AND is_unread = :unread");
        }
        sql.append(" ORDER BY created_at DESC LIMIT :limit OFFSET :offset");

        var spec = r2dbcTemplate.getDatabaseClient().sql(sql.toString())
                .bind("recipient", username)
                .bind("limit", limit)
                .bind("offset", offset);
        if (unread != null) {
            spec = spec.bind("unread", unread);
        }
        return spec.map((row, meta) -> mapToNotification(row)).all();
    }

    @Override
    public Mono<Long> countUnread(String username) {
        return r2dbcTemplate.getDatabaseClient()
                .sql("SELECT COUNT(*) FROM notifications WHERE recipient = :recipient AND is_unread = true")
                .bind("recipient", username)
                .map(row -> row.get(0, Long.class))
                .one()
                .defaultIfEmpty(0L);
    }

    @Override
    @Transactional
    public Mono<Void> markAsRead(String username, Long notificationId) {
        return r2dbcTemplate.getDatabaseClient()
                .sql("UPDATE notifications SET is_unread = false, read_at = :now WHERE id = :id AND recipient = :recipient")
                .bind("id", notificationId)
                .bind("recipient", username)
                .bind("now", Instant.now())
                .then();
    }

    @Override
    @Transactional
    public Mono<Void> markAsRead(String username, Iterable<Long> notificationIds) {
        return Flux.fromIterable(notificationIds)
                .flatMap(id -> markAsRead(username, id))
                .then();
    }

    @Override
    @Transactional
    public Mono<Void> delete(String username, Long notificationId) {
        return r2dbcTemplate.getDatabaseClient()
                .sql("DELETE FROM notifications WHERE id = :id AND recipient = :recipient")
                .bind("id", notificationId)
                .bind("recipient", username)
                .then();
    }

    @Override
    @Transactional
    public Mono<Void> delete(String username, Iterable<Long> notificationIds) {
        return Flux.fromIterable(notificationIds)
                .flatMap(id -> delete(username, id))
                .then();
    }

    private Notification mapToNotification(io.r2dbc.spi.Row row) {
        var n = new Notification();
        n.setId(row.get("id", Long.class));
        n.setRecipient(row.get("recipient", String.class));
        n.setCategory(row.get("category", String.class));
        n.setMessageKey(row.get("message_key", String.class));
        var argsObj = row.get("message_args");
        if (argsObj != null) {
            log.info("message_args type: {}, value: {}", argsObj.getClass().getName(), argsObj);
            if (argsObj instanceof Map<?, ?> m) {
                n.setMessageArgs((Map<String, Object>) m);
            } else if (argsObj instanceof String s && StringUtils.hasText(s) && !"{}".equals(s)) {
                try {
                    var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    n.setMessageArgs(mapper.readValue(s,
                            new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {}));
                } catch (Exception e) {
                    log.warn("Failed to deserialize message_args (type={}): {}",
                            argsObj.getClass().getName(), s, e);
                }
            }
        }
        n.setSubjectUrl(row.get("subject_url", String.class));
        n.setUnread(Boolean.TRUE.equals(row.get("is_unread", Boolean.class)));
        n.setCreatedAt(convertToInstant(row.get("created_at")));
        n.setReadAt(convertToInstant(row.get("read_at")));
        return n;
    }

    private Long convertToLong(Object value) {
        if (value instanceof Long l) return l;
        if (value instanceof Integer i) return i.longValue();
        if (value instanceof Number n) return n.longValue();
        return null;
    }

    private Instant convertToInstant(Object value) {
        if (value instanceof Instant i) return i;
        if (value instanceof java.time.LocalDateTime ldt) return ldt.atZone(java.time.ZoneOffset.UTC).toInstant();
        return null;
    }
}
