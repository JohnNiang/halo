package run.halo.app.notification;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;
import reactor.core.publisher.Mono;

/**
 * Loads notification categories from YAML files on the classpath and from plugin resources.
 *
 * @author johnniang
 * @since 2.20.0
 */
@Slf4j
@Component
public class DefaultNotificationCategoryRegistry implements NotificationCategoryRegistry {

    private final Map<String, NotificationCategory> categories = new ConcurrentHashMap<>();

    public DefaultNotificationCategoryRegistry() {
        loadCategories();
    }

    @SuppressWarnings("unchecked")
    private void loadCategories() {
        try {
            var resolver = new PathMatchingResourcePatternResolver();
            var resources = resolver.getResources("classpath*:notification-categories.yaml");
            var yaml = new Yaml();
            for (Resource resource : resources) {
                try (InputStream in = resource.getInputStream()) {
                    Map<String, Object> data = yaml.load(in);
                    if (data == null) continue;
                    var categoryList = (List<Map<String, Object>>) data.get("categories");
                    if (categoryList == null) continue;
                    for (Map<String, Object> entry : categoryList) {
                        var category = new NotificationCategory(
                                (String) entry.get("name"),
                                (String) entry.get("displayName"),
                                (String) entry.getOrDefault("description", ""),
                                (String) entry.getOrDefault("uiPermission", null),
                                Boolean.TRUE.equals(entry.get("hidden"))
                        );
                        categories.put(category.name(), category);
                        log.info("Registered notification category: {}", category.name());
                    }
                } catch (IOException e) {
                    log.warn("Failed to load notification categories from {}", resource, e);
                }
            }
        } catch (IOException e) {
            log.error("Failed to scan for notification-categories.yaml files", e);
        }
        log.info("Loaded {} notification categories", categories.size());
    }

    @Override
    public Mono<List<NotificationCategory>> getCategories() {
        return Mono.fromSupplier(() -> categories.values().stream()
                .filter(c -> !c.hidden())
                .toList());
    }

    @Override
    public Mono<NotificationCategory> getCategory(String name) {
        return Mono.fromSupplier(() -> categories.get(name));
    }

    @Override
    public Mono<Boolean> exists(String name) {
        return Mono.fromSupplier(() -> categories.containsKey(name));
    }
}
