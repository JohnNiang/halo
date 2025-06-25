package run.halo.app.security;

import java.time.Instant;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.User;
import run.halo.app.core.extension.User.UserSpec;
import run.halo.app.core.user.service.UserService;
import run.halo.app.extension.Metadata;
import run.halo.app.extension.ReactiveExtensionClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultSuperAdminInitializer implements SuperAdminInitializer {

    private final ReactiveExtensionClient client;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;

    @Override
    public Mono<Void> initialize(InitializationParam param) {
        return client.fetch(User.class, param.getUsername())
            .switchIfEmpty(
                Mono.defer(() -> client.create(
                        createAdmin(param.getUsername(), param.getPassword(), param.getEmail())
                    ))
                    .flatMap(admin -> userService.grantRoles(
                        admin.getMetadata().getName(), Set.of(SUPER_ROLE_NAME)
                    ))
            )
            .then();
    }

    User createAdmin(String username, String password, String email) {
        var metadata = new Metadata();
        metadata.setName(username);

        var spec = new UserSpec();
        spec.setDisplayName("Administrator");
        spec.setDisabled(false);
        spec.setRegisteredAt(Instant.now());
        spec.setTwoFactorAuthEnabled(false);
        spec.setEmail(email);
        spec.setPassword(passwordEncoder.encode(password));

        var user = new User();
        user.setMetadata(metadata);
        user.setSpec(spec);
        return user;
    }
}
