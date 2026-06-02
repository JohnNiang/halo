package run.halo.app.notification;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

/**
 * Unit tests for {@link DefaultNotificationCategoryRegistry}.
 *
 * @author johnniang
 * @since 2.20.0
 */
class DefaultNotificationCategoryRegistryTest {

    private final DefaultNotificationCategoryRegistry registry = new DefaultNotificationCategoryRegistry();

    @Test
    void shouldLoadCategoriesFromYaml() {
        StepVerifier.create(registry.getCategories())
                .assertNext(categories -> {
                    assertThat(categories).isNotEmpty();
                    assertThat(categories).anyMatch(c -> "new-comment-on-post".equals(c.name()));
                    assertThat(categories).anyMatch(c -> "someone-replied-to-you".equals(c.name()));
                    // hidden categories should not appear
                    assertThat(categories).noneMatch(c -> "email-verification".equals(c.name()));
                })
                .verifyComplete();
    }

    @Test
    void existsShouldReturnTrueForKnownCategory() {
        StepVerifier.create(registry.exists("new-comment-on-post"))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void existsShouldReturnFalseForUnknownCategory() {
        StepVerifier.create(registry.exists("nonexistent")).expectNext(false).verifyComplete();
    }

    @Test
    void getCategoryShouldReturnKnownCategory() {
        StepVerifier.create(registry.getCategory("new-device-login"))
                .assertNext(c -> {
                    assertThat(c.name()).isEqualTo("new-device-login");
                    assertThat(c.hidden()).isFalse();
                })
                .verifyComplete();
    }

    @Test
    void getCategoryShouldReturnNullForUnknown() {
        StepVerifier.create(registry.getCategory("unknown")).expectNextCount(0).verifyComplete();
    }

    @Test
    void hiddenCategoriesShouldNotBeInList() {
        StepVerifier.create(registry.getCategories())
                .assertNext(categories -> {
                    assertThat(categories).noneMatch(c -> c.hidden());
                    assertThat(categories).noneMatch(c -> "email-verification".equals(c.name()));
                    assertThat(categories).noneMatch(c -> "reset-password-by-email".equals(c.name()));
                })
                .verifyComplete();
    }
}
