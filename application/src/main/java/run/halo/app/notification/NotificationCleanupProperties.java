package run.halo.app.notification;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "halo.notification.cleanup")
public class NotificationCleanupProperties {

    private boolean enabled = false;

    private int retentionDays = 30;
}
