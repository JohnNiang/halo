package run.halo.app.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.mail.javamail.JavaMailSender;
import reactor.test.StepVerifier;
import run.halo.app.extension.ReactiveExtensionClient;

/**
 * Unit tests for {@link EmailNotifier}.
 *
 * @author johnniang
 * @since 2.20.0
 */
@ExtendWith(MockitoExtension.class)
class EmailNotifierTest {

    @Mock
    private EmailSenderHelper emailSenderHelper;

    @Mock
    private ReactiveExtensionClient client;

    @Mock
    private JavaMailSender javaMailSender;

    private MessageSource messageSource;

    private EmailNotifier emailNotifier;

    @BeforeEach
    void setUp() {
        var bundleMsgSrc = new ResourceBundleMessageSource();
        bundleMsgSrc.setBasename("config.i18n.messages");
        bundleMsgSrc.setDefaultEncoding("UTF-8");
        this.messageSource = bundleMsgSrc;

        emailNotifier = new EmailNotifier(emailSenderHelper, client, messageSource);
    }

    @Test
    void nameShouldReturnEmailNotifier() {
        assertThat(emailNotifier.name()).isEqualTo("email-notifier");
    }

    @Test
    void supportsShouldReturnFalseWhenUserNotFound() {
        when(client.fetch(run.halo.app.core.extension.User.class, "unknown"))
                .thenReturn(reactor.core.publisher.Mono.empty());

        StepVerifier.create(emailNotifier.supports("unknown")).expectNext(false).verifyComplete();
    }
}
