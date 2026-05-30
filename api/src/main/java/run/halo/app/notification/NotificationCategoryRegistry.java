package run.halo.app.notification;

import java.util.List;
import reactor.core.publisher.Mono;

/**
 * Registry of available notification categories, loaded from YAML files
 * in core and plugin resources.
 *
 * @author johnniang
 * @since 2.20.0
 */
public interface NotificationCategoryRegistry {

    /**
     * Returns all registered categories, excluding hidden ones.
     */
    Mono<List<NotificationCategory>> getCategories();

    /**
     * Returns a specific category by name.
     */
    Mono<NotificationCategory> getCategory(String name);

    /**
     * Returns whether a category with the given name exists.
     */
    Mono<Boolean> exists(String name);
}
