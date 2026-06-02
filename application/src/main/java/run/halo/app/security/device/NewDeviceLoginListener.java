package run.halo.app.security.device;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.halo.app.notification.NotificationRequest;
import run.halo.app.notification.NotificationService;

/**
 * Sends a notification when a new device logs in.
 *
 * @author guqing
 * @since 2.17.0
 */
@Component
@RequiredArgsConstructor
public class NewDeviceLoginListener {

    private static final String REASON_TYPE = "new-device-login";
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss O").withZone(ZoneOffset.systemDefault());

    private final NotificationService notificationService;

    @EventListener
    Mono<Void> onApplicationEvent(NewDeviceLoginEvent event) {
        var device = event.getDevice();
        var principalName = device.getSpec().getPrincipalName();
        return notificationService.notify(new NotificationRequest(
                Set.of(principalName),
                REASON_TYPE,
                "notification.new-device-login",
                Map.of(
                        "os", device.getStatus().getOs(),
                        "browser", device.getStatus().getBrowser(),
                        "ipAddress", device.getSpec().getIpAddress(),
                        "loginTime", DATE_TIME_FORMATTER.format(device.getSpec().getLastAuthenticatedTime())),
                null));
    }
}
