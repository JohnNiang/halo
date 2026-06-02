package run.halo.app.notification;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.data.util.Pair;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import run.halo.app.core.extension.User;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.extension.Secret;
import run.halo.app.infra.utils.JsonUtils;
import run.halo.app.notification.EmailSenderHelper.EmailSenderConfig;

/**
 * Email notifier that sends notifications via email. SMTP configuration is stored in a Secret named
 * "email-notifier-config".
 *
 * @author guqing
 * @since 2.10.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailNotifier implements ReactiveNotifier {

    private static final String SECRET_NAME = "email-notifier-config";
    private static final String SENDER_KEY = "sender";

    private final EmailSenderHelper emailSenderHelper;
    private final ReactiveExtensionClient client;
    private final MessageSource messageSource;
    private final AtomicReference<Pair<EmailSenderConfig, JavaMailSender>> senderRef = new AtomicReference<>();

    @Override
    public String name() {
        return "email-notifier";
    }

    @Override
    public Mono<Void> notify(Notification notification) {
        return fetchSenderConfig().flatMap(config -> {
            if (!config.isEnable()) {
                log.debug("Email notifier is disabled, skipping.");
                return Mono.empty();
            }
            return resolveEmail(notification.getRecipient()).flatMap(toEmail -> {
                var sender = getOrCreateMailSender(config);
                var preparator = createMessage(config, toEmail, notification);
                return Mono.fromRunnable(() -> sender.send(preparator))
                        .subscribeOn(Schedulers.boundedElastic())
                        .then();
            });
        });
    }

    @Override
    public Mono<Boolean> supports(String recipient) {
        return resolveEmail(recipient).hasElement();
    }

    private Mono<String> resolveEmail(String username) {
        return client.fetch(User.class, username)
                .filter(user -> user.getSpec().isEmailVerified())
                .map(user -> user.getSpec().getEmail());
    }

    private Mono<EmailSenderConfig> fetchSenderConfig() {
        return client.fetch(Secret.class, SECRET_NAME)
                .map(secret -> {
                    var configData = secret.getStringData();
                    if (configData == null || !configData.containsKey(SENDER_KEY)) {
                        var disabled = new EmailSenderConfig();
                        disabled.setEnable(false);
                        return disabled;
                    }
                    return JsonUtils.jsonToObject(configData.get(SENDER_KEY), EmailSenderConfig.class);
                })
                .defaultIfEmpty(new EmailSenderConfig());
    }

    private JavaMailSender getOrCreateMailSender(EmailSenderConfig config) {
        return senderRef
                .updateAndGet(pair -> {
                    if (pair != null && pair.getFirst().equals(config)) {
                        return pair;
                    }
                    return Pair.of(config, emailSenderHelper.createJavaMailSender(config));
                })
                .getSecond();
    }

    private MimeMessagePreparator createMessage(EmailSenderConfig config, String toEmail, Notification notification) {
        var title = renderTitle(notification);
        var body = renderBody(notification);
        return mimeMessage -> {
            var helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(config.getSender(), config.getDisplayName());
            helper.setSubject(title);
            helper.setText(body, buildHtml(title, body));
            helper.setTo(toEmail);
        };
    }

    private String renderTitle(Notification notification) {
        return messageSource.getMessage(
                notification.getMessageKey() + ".title",
                toArgs(notification.getMessageArgs()),
                "Notification",
                Locale.getDefault());
    }

    private String renderBody(Notification notification) {
        return messageSource.getMessage(
                notification.getMessageKey() + ".body", toArgs(notification.getMessageArgs()), "", Locale.getDefault());
    }

    private Object[] toArgs(java.util.Map<String, Object> args) {
        if (args == null || args.isEmpty()) {
            return new Object[0];
        }
        return args.values().toArray();
    }

    private String buildHtml(String title, String body) {
        return """
                <html><body>
                <h2>%s</h2>
                <p>%s</p>
                </body></html>
                """.formatted(escapeHtml(title), escapeHtml(body));
    }

    private String escapeHtml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
