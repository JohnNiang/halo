package run.halo.app.notification;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.halo.app.plugin.extensionpoint.ExtensionGetter;

/**
 * Unit tests for {@link DefaultNotificationDispatchService}.
 *
 * @author johnniang
 * @since 2.20.0
 */
@ExtendWith(MockitoExtension.class)
class DefaultNotificationDispatchServiceTest {

    @Mock
    private R2dbcEntityTemplate r2dbcTemplate;

    @Mock
    private ExtensionGetter extensionGetter;

    @Mock
    private NotificationPreferenceService preferenceService;

    private DefaultNotificationDispatchService dispatchService;

    @BeforeEach
    void setUp() {
        dispatchService = new DefaultNotificationDispatchService(r2dbcTemplate, extensionGetter, preferenceService);
    }

    @Test
    void dispatchWithNoEnabledNotifiersShouldSkip() {
        var notification = new Notification();
        notification.setId(1L);
        notification.setRecipient("user1");
        notification.setCategory("test-category");

        when(preferenceService.getEnabledNotifiers("user1", "test-category")).thenReturn(Mono.just(Set.of()));
        when(extensionGetter.getExtensionList(ReactiveNotifier.class)).thenReturn(List.of());

        StepVerifier.create(dispatchService.dispatch(notification)).verifyComplete();

        verify(preferenceService).getEnabledNotifiers("user1", "test-category");
    }

    @Test
    void dispatchWithNoNotifiersRegisteredShouldSkip() {
        var notification = new Notification();
        notification.setId(1L);
        notification.setRecipient("user1");
        notification.setCategory("test-category");

        when(preferenceService.getEnabledNotifiers("user1", "test-category")).thenReturn(Mono.just(Set.of()));
        when(extensionGetter.getExtensionList(ReactiveNotifier.class)).thenReturn(List.of());

        StepVerifier.create(dispatchService.dispatch(notification)).verifyComplete();
    }
}
