package run.halo.app.notification.endpoint;

import io.swagger.v3.oas.annotations.enums.ParameterIn;
import java.util.List;
import java.util.Map;
import org.springdoc.core.fn.builders.parameter.Builder;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.halo.app.notification.NotificationService;

import static org.springdoc.webflux.core.fn.SpringdocRouteBuilder.route;

@Component
public class UserNotificationEndpoint {

    private final NotificationService notificationService;

    public UserNotificationEndpoint(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Bean
    RouterFunction<ServerResponse> userNotificationEndpoints() {
        var tag = "uc.api.halo.run/v1alpha1/Notification";
        return route()
                .GET("/apis/uc.api.halo.run/v1alpha1/notifications", this::listNotifications, builder -> {
                    builder.operationId("ListNotifications")
                            .tag(tag)
                            .parameter(Builder.parameterBuilder().name("unread").in(ParameterIn.QUERY).implementation(Boolean.class))
                            .parameter(Builder.parameterBuilder().name("page").in(ParameterIn.QUERY).implementation(Integer.class))
                            .parameter(Builder.parameterBuilder().name("size").in(ParameterIn.QUERY).implementation(Integer.class));
                })
                .GET("/apis/uc.api.halo.run/v1alpha1/notifications/unread-count", this::unreadCount, builder -> {
                    builder.operationId("GetUnreadNotificationCount").tag(tag);
                })
                .PUT("/apis/uc.api.halo.run/v1alpha1/notifications/{id}/mark-as-read", this::markAsRead, builder -> {
                    builder.operationId("MarkNotificationAsRead").tag(tag)
                            .parameter(Builder.parameterBuilder().name("id").in(ParameterIn.PATH).implementation(Long.class));
                })
                .PUT("/apis/uc.api.halo.run/v1alpha1/notifications/mark-as-read", this::markBatchAsRead, builder -> {
                    builder.operationId("MarkNotificationsAsRead").tag(tag);
                })
                .DELETE("/apis/uc.api.halo.run/v1alpha1/notifications/{id}", this::deleteNotification, builder -> {
                    builder.operationId("DeleteNotification").tag(tag)
                            .parameter(Builder.parameterBuilder().name("id").in(ParameterIn.PATH).implementation(Long.class));
                })
                .DELETE("/apis/uc.api.halo.run/v1alpha1/notifications", this::deleteBatch, builder -> {
                    builder.operationId("DeleteNotifications").tag(tag);
                })
                .build();
    }

    private Mono<ServerResponse> listNotifications(ServerRequest request) {
        return getUsername()
                .flatMapMany(username -> {
                    var unread = request.queryParam("unread").map(Boolean::parseBoolean).orElse(null);
                    var page = request.queryParam("page").map(Integer::parseInt).orElse(0);
                    var size = request.queryParam("size").map(Integer::parseInt).orElse(20);
                    return notificationService.listByUser(username, unread, page * size, size);
                })
                .collectList()
                .flatMap(notifications -> ServerResponse.ok().bodyValue(notifications));
    }

    private Mono<ServerResponse> unreadCount(ServerRequest request) {
        return getUsername()
                .flatMap(notificationService::countUnread)
                .flatMap(count -> ServerResponse.ok().bodyValue(Map.of("count", count)));
    }

    private Mono<ServerResponse> markAsRead(ServerRequest request) {
        var id = Long.parseLong(request.pathVariable("id"));
        return getUsername()
                .flatMap(username -> notificationService.markAsRead(username, id))
                .then(ServerResponse.ok().build());
    }

    @SuppressWarnings("unchecked")
    private Mono<ServerResponse> markBatchAsRead(ServerRequest request) {
        return request.bodyToMono(Map.class)
                .flatMap(body -> {
                    var ids = ((List<Number>) body.get("ids")).stream().map(Number::longValue).toList();
                    return getUsername().flatMap(username -> notificationService.markAsRead(username, ids));
                })
                .then(ServerResponse.ok().build());
    }

    private Mono<ServerResponse> deleteNotification(ServerRequest request) {
        var id = Long.parseLong(request.pathVariable("id"));
        return getUsername()
                .flatMap(username -> notificationService.delete(username, id))
                .then(ServerResponse.ok().build());
    }

    @SuppressWarnings("unchecked")
    private Mono<ServerResponse> deleteBatch(ServerRequest request) {
        return request.bodyToMono(Map.class)
                .flatMap(body -> {
                    var ids = ((List<Number>) body.get("ids")).stream().map(Number::longValue).toList();
                    return getUsername().flatMap(username -> notificationService.delete(username, ids));
                })
                .then(ServerResponse.ok().build());
    }

    private Mono<String> getUsername() {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication())
                .map(Authentication::getName);
    }
}
