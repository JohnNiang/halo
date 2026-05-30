package run.halo.app.notification;

import java.util.Map;
import java.util.Set;
import reactor.core.publisher.Mono;

/**
 * Manages per-user, per-category, per-notifier notification preferences.
 *
 * @author johnniang
 * @since 2.20.0
 */
public interface NotificationPreferenceService {

    /**
     * Returns enabled notifier names per category for the given user.
     * If no preferences are saved, all notifiers are considered enabled for all categories.
     */
    Mono<Map<String, Set<String>>> getPreferences(String username);

    /**
     * Saves preferences. The map is category → list of enabled notifier names.
     */
    Mono<Void> savePreferences(String username, Map<String, Set<String>> preferences);

    /**
     * Returns the list of enabled notifier names for a user and category.
     */
    Mono<Set<String>> getEnabledNotifiers(String username, String category);
}
