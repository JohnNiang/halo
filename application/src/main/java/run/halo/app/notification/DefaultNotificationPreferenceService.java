package run.halo.app.notification;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

/**
 * Default implementation of {@link NotificationPreferenceService}.
 *
 * @author johnniang
 * @since 2.20.0
 */
@Service
@RequiredArgsConstructor
public class DefaultNotificationPreferenceService implements NotificationPreferenceService {

    private final R2dbcEntityTemplate r2dbcTemplate;

    @Override
    public Mono<Map<String, Set<String>>> getPreferences(String username) {
        return r2dbcTemplate
                .getDatabaseClient()
                .sql("SELECT category, notifier, enabled FROM notification_preferences WHERE user_id = :userId")
                .bind("userId", username)
                .map(row -> Map.entry(
                        (String) row.get("category"),
                        Map.entry((String) row.get("notifier"), Boolean.TRUE.equals(row.get("enabled")))))
                .all()
                .collectList()
                .map(rows -> {
                    Map<String, Set<String>> result = new HashMap<>();
                    for (var entry : rows) {
                        var category = entry.getKey();
                        var notifierEntry = entry.getValue();
                        if (notifierEntry.getValue()) {
                            result.computeIfAbsent(category, k -> new HashSet<>())
                                    .add(notifierEntry.getKey());
                        }
                    }
                    return result;
                });
    }

    @Override
    @Transactional
    public Mono<Void> savePreferences(String username, Map<String, Set<String>> preferences) {
        return r2dbcTemplate
                .getDatabaseClient()
                .sql("DELETE FROM notification_preferences WHERE user_id = :userId")
                .bind("userId", username)
                .then()
                .then(Mono.defer(() -> {
                    var inserts = preferences.entrySet().stream()
                            .flatMap(catEntry -> catEntry.getValue().stream()
                                    .map(notifier -> Map.entry(catEntry.getKey(), notifier)))
                            .map(e -> r2dbcTemplate
                                    .getDatabaseClient()
                                    .sql(
                                            "INSERT INTO notification_preferences (user_id, category, notifier, enabled) VALUES (:userId, :category, :notifier, true)")
                                    .bind("userId", username)
                                    .bind("category", e.getKey())
                                    .bind("notifier", e.getValue())
                                    .then())
                            .toList();
                    return Mono.when(inserts);
                }));
    }

    @Override
    public Mono<Set<String>> getEnabledNotifiers(String username, String category) {
        return r2dbcTemplate
                .getDatabaseClient()
                .sql(
                        "SELECT notifier FROM notification_preferences WHERE user_id = :userId AND category = :category AND enabled = true")
                .bind("userId", username)
                .bind("category", category)
                .map(row -> (String) row.get("notifier"))
                .all()
                .collectList()
                .map(notifiers -> (Set<String>) new HashSet<>(notifiers));
    }
}
