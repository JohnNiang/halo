package run.halo.app.notification.endpoint;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.halo.app.notification.NotificationCategoryRegistry;
import run.halo.app.notification.NotificationPreferenceService;
import run.halo.app.notification.ReactiveNotifier;
import run.halo.app.plugin.extensionpoint.ExtensionGetter;

import static org.springdoc.webflux.core.fn.SpringdocRouteBuilder.route;

@Component
public class UserNotificationPreferenceEndpoint {

    private final NotificationPreferenceService preferenceService;
    private final NotificationCategoryRegistry categoryRegistry;
    private final ExtensionGetter extensionGetter;

    public UserNotificationPreferenceEndpoint(NotificationPreferenceService preferenceService,
                                              NotificationCategoryRegistry categoryRegistry,
                                              ExtensionGetter extensionGetter) {
        this.preferenceService = preferenceService;
        this.categoryRegistry = categoryRegistry;
        this.extensionGetter = extensionGetter;
    }

    @Bean
    RouterFunction<ServerResponse> userNotificationPreferenceEndpoints() {
        var tag = "uc.api.halo.run/v1alpha1/Notification";
        return route()
                .GET("/apis/uc.api.halo.run/v1alpha1/notification-preferences", this::getPreferences, builder -> {
                    builder.operationId("GetNotificationPreferences").tag(tag);
                })
                .PUT("/apis/uc.api.halo.run/v1alpha1/notification-preferences", this::savePreferences, builder -> {
                    builder.operationId("SaveNotificationPreferences").tag(tag);
                })
                .build();
    }

    private Mono<ServerResponse> getPreferences(ServerRequest request) {
        return getUsername().flatMap(username ->
                preferenceService.getPreferences(username)
                        .flatMap(prefs ->
                                categoryRegistry.getCategories().map(categories -> {
                                    var notifiers = extensionGetter.getExtensionList(ReactiveNotifier.class)
                                            .stream()
                                            .map(ReactiveNotifier::name)
                                            .toList();
                                    return Map.of(
                                            "categories", categories,
                                            "notifiers", notifiers,
                                            "preferences", prefs
                                    );
                                })
                        )
                        .flatMap(result -> ServerResponse.ok().bodyValue(result))
        );
    }

    @SuppressWarnings("unchecked")
    private Mono<ServerResponse> savePreferences(ServerRequest request) {
        return request.bodyToMono(Map.class)
                .flatMap(body -> getUsername().flatMap(username -> {
                    var rawPrefs = (Map<String, ?>)(Map) body;
                    Map<String, Set<String>> preferences = new java.util.HashMap<>();
                    rawPrefs.forEach((key, value) -> {
                        if (value instanceof java.util.List<?> list) {
                            preferences.put(key, new java.util.HashSet<>((java.util.List<String>) list));
                        }
                    });
                    return preferenceService.savePreferences(username, preferences);
                }))
                .then(ServerResponse.ok().build());
    }

    private Mono<String> getUsername() {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication())
                .map(Authentication::getName);
    }
}
