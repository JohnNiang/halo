package run.halo.app.security.authentication.mfa;

import static org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers.pathMatchers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationFailureHandler;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import run.halo.app.security.authentication.login.HaloUser;

@Slf4j
public class TotpAuthenticationFilter extends AuthenticationWebFilter {

    public TotpAuthenticationFilter(ServerSecurityContextRepository securityContextRepository) {
        super(new MfaAuthenticationManager());
        setSecurityContextRepository(securityContextRepository);
        setRequiresAuthenticationMatcher(pathMatchers(HttpMethod.POST, "/login/mfa/totp"));
        setServerAuthenticationConverter(new TotpCodeAuthenticationConverter());
        setAuthenticationSuccessHandler(new RedirectServerAuthenticationSuccessHandler("/console"));
        setAuthenticationFailureHandler(new RedirectServerAuthenticationFailureHandler("/console/login/mfa?failed"));
    }

    private static class TotpCodeAuthenticationConverter implements ServerAuthenticationConverter {

        private final String codeParameter = "code";

        @Override
        public Mono<Authentication> convert(ServerWebExchange exchange) {
            // Check the request is authenticated before.
            return ReactiveSecurityContextHolder.getContext()
                    .map(SecurityContext::getAuthentication)
                    .filter(MfaAuthentication.class::isInstance)
                    .switchIfEmpty(Mono.error(() -> new MfaAuthenticationException("MFA Authentication required.")))
                    .flatMap(mfaAuthentication -> exchange.getFormData()
                            .map(formData -> {
                                var code = formData.getFirst(codeParameter);
                                return new TotpAuthenticationToken(code);
                            }));
        }
    }

    private static class MfaAuthenticationException extends AuthenticationException {

        public MfaAuthenticationException(String msg, Throwable cause) {
            super(msg, cause);
        }

        public MfaAuthenticationException(String msg) {
            super(msg);
        }

    }

    private static class MfaAuthenticationManager implements ReactiveAuthenticationManager {

        @Override
        public Mono<Authentication> authenticate(Authentication authentication) {
            var code = (String) authentication.getCredentials();
            log.debug("Got TOTP code {}", code);
            // get user details
            return ReactiveSecurityContextHolder.getContext()
                    .map(SecurityContext::getAuthentication)
                    .cast(MfaAuthentication.class)
                    .map(MfaAuthentication::getPrevious)
                    .flatMap(previousAuth -> {
                        var principal = previousAuth.getPrincipal();
                        if (principal instanceof HaloUser haloUser) {
                            // TODO Check the code
                            return Mono.just(previousAuth);
                        }
                        return Mono.error(new MfaAuthenticationException("Invalid previous authentication."));
                    });
        }
    }
}
