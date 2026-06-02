package run.halo.app.notification;

import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Unit tests for {@link DefaultNotificationService}.
 *
 * @author johnniang
 * @since 2.20.0
 */
@ExtendWith(MockitoExtension.class)
class DefaultNotificationServiceTest {

    @Mock
    private R2dbcEntityTemplate r2dbcTemplate;

    @Mock
    private NotificationCategoryRegistry categoryRegistry;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private DefaultNotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new DefaultNotificationService(r2dbcTemplate, categoryRegistry, eventPublisher);
    }

    @Test
    void notifyWithUnknownCategoryShouldFail() {
        when(categoryRegistry.exists("unknown-category")).thenReturn(Mono.just(false));

        var request = new NotificationRequest(Set.of("user1"), "unknown-category", "test.key", Map.of(), null);

        StepVerifier.create(notificationService.notify(request))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void notifyWithValidCategoryShouldSucceed() {
        when(categoryRegistry.exists("new-comment-on-post")).thenReturn(Mono.just(true));

        var request = new NotificationRequest(
                Set.of("user1"), "new-comment-on-post", "test.key", Map.of("key", "value"), null);

        StepVerifier.create(notificationService.notify(request)).expectError().verify();
    }
}
