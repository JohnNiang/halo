package run.halo.app.core.user.service.impl;

import java.time.Clock;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.token.Sha512DigestUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.User;
import run.halo.app.core.user.service.*;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.infra.ExternalLinkProcessor;
import run.halo.app.notification.NotificationRequest;
import run.halo.app.notification.NotificationService;

/**
 * A default implementation for {@link EmailPasswordRecoveryService}.
 *
 * @author guqing
 * @since 2.11.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailPasswordRecoveryServiceImpl implements EmailPasswordRecoveryService {

    public static final int MAX_ATTEMPTS = 5;
    public static final long LINK_EXPIRATION_MINUTES = 30;
    static final String RESET_PASSWORD_BY_EMAIL_REASON_TYPE = "reset-password-by-email";
    private static final Duration RESET_TOKEN_LIFE_TIME = Duration.ofMinutes(LINK_EXPIRATION_MINUTES);

    private final ExternalLinkProcessor externalLinkProcessor;
    private final ReactiveExtensionClient client;
    private final UserService userService;
    private final ResetTokenRepository resetTokenRepository;
    private final NotificationService notificationService;

    private Clock clock = Clock.systemDefaultZone();

    @Override
    public Mono<Void> sendPasswordResetEmail(String username, String email) {
        return client.fetch(User.class, username).flatMap(user -> {
            var userEmail = user.getSpec().getEmail();
            if (!StringUtils.equals(userEmail, email)) {
                return Mono.empty();
            }
            if (!user.getSpec().isEmailVerified()) {
                return Mono.empty();
            }
            return sendResetPasswordNotification(username, email);
        });
    }

    @Override
    public Mono<Void> sendPasswordResetEmail(String email) {
        if (StringUtils.isBlank(email)) {
            return Mono.empty();
        }
        return userService
                .listByEmail(email)
                .filter(user -> user.getSpec().isEmailVerified())
                .next()
                .flatMap(
                        user -> sendResetPasswordNotification(user.getMetadata().getName(), email));
    }

    @Override
    public Mono<Void> changePassword(String newPassword, String token) {
        Assert.state(StringUtils.isNotBlank(newPassword), "NewPassword must not be blank");
        Assert.state(StringUtils.isNotBlank(token), "Token for reset password must not be blank");
        var tokenHash = hashToken(token);
        return getValidResetToken(token)
                .flatMap(resetToken -> userService
                        .updateWithRawPassword(resetToken.username(), newPassword)
                        .then(resetTokenRepository.removeByTokenHash(tokenHash)));
    }

    @Override
    public Mono<ResetToken> getValidResetToken(String token) {
        return resetTokenRepository
                .findByTokenHash(hashToken(token))
                .filter(resetToken -> clock.instant().isBefore(resetToken.expiresAt()))
                .switchIfEmpty(Mono.error(InvalidResetTokenException::new));
    }

    private Mono<Void> sendResetPasswordNotification(String username, String email) {
        var token = generateToken();
        var tokenHash = hashToken(token);
        var expiresAt = clock.instant().plus(RESET_TOKEN_LIFE_TIME);
        var uri = UriComponentsBuilder.fromUriString("/")
                .pathSegment("password-reset", "email", token)
                .build(true)
                .toUri();
        var resetToken = new ResetToken(tokenHash, username, expiresAt);
        return resetTokenRepository
                .save(resetToken)
                .then(externalLinkProcessor.processLink(uri))
                .flatMap(link -> {
                    log.debug(
                            "Generated reset password token for user '{}' and email '{}': {}", username, email, token);
                    return notificationService.notify(new NotificationRequest(
                            java.util.Set.of(username),
                            RESET_PASSWORD_BY_EMAIL_REASON_TYPE,
                            "notification.reset-password-by-email",
                            java.util.Map.of("link", link, "expirationAtMinutes", LINK_EXPIRATION_MINUTES),
                            null));
                });
    }

    private static String hashToken(String token) {
        return Sha512DigestUtils.shaHex(token);
    }

    private static String generateToken() {
        return RandomStringUtils.secure().nextAlphanumeric(64);
    }
}
