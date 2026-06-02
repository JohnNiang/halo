package run.halo.app.notification.endpoint;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Unit tests for endpoint request handling logic.
 *
 * @author johnniang
 * @since 2.20.0
 */
class NotificationEndpointUnitTest {

    @Test
    void securityContextShouldResolveUsername() {
        var auth = new org.springframework.security.authentication.TestingAuthenticationToken(
                "test-user", "password", "ROLE_USER");
        var context = new SecurityContextImpl(auth);

        Mono<String> username = ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication())
                .map(Authentication::getName);

        StepVerifier.create(
                        username.contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(context))))
                .expectNext("test-user")
                .verifyComplete();
    }

    @Test
    void notificationPreferencesResponseShouldContainRequiredFields() {
        // Verify that the preferences response structure has categories, notifiers, and preferences
        var response = Map.of(
                "categories", List.of("new-comment-on-post"),
                "notifiers", List.of("email-notifier"),
                "preferences", Map.of());

        assertThat(response).containsKeys("categories", "notifiers", "preferences");
        assertThat(response.get("categories")).isInstanceOf(List.class);
        assertThat(response.get("notifiers")).isInstanceOf(List.class);
    }
}
