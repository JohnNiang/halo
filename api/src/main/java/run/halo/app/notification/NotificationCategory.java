package run.halo.app.notification;

/**
 * A notification category definition from the registry.
 *
 * @param name unique category key (e.g., "new-comment-on-post")
 * @param displayName human-readable name for UI
 * @param description human-readable description
 * @param uiPermission optional UI permission required to see this category in preferences
 * @param hidden if true, this category is not shown in the preference matrix
 * @author johnniang
 * @since 2.20.0
 */
public record NotificationCategory(
        String name, String displayName, String description, String uiPermission, boolean hidden) {}
