package run.halo.app.notification;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link NotificationCleanupProperties} and {@link NotificationCleanupTask}.
 *
 * @author johnniang
 * @since 2.20.0
 */
class NotificationCleanupTaskTest {

    @Test
    void defaultPropertiesShouldBeDisabled() {
        var props = new NotificationCleanupProperties();
        assertThat(props.isEnabled()).isFalse();
        assertThat(props.getRetentionDays()).isEqualTo(30);
    }

    @Test
    void propertySettersShouldWork() {
        var props = new NotificationCleanupProperties();
        props.setEnabled(true);
        props.setRetentionDays(60);

        assertThat(props.isEnabled()).isTrue();
        assertThat(props.getRetentionDays()).isEqualTo(60);
    }
}
