package run.halo.app.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.server.resource.web.access.server.BearerTokenServerAccessDeniedHandler;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.security.web.server.authorization.ServerWebExchangeDelegatingServerAccessDeniedHandler;
import org.springframework.security.web.server.authorization.ServerWebExchangeDelegatingServerAccessDeniedHandler.DelegateEntry;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import run.halo.app.security.authentication.SecurityConfigurer;
import run.halo.app.security.authentication.mfa.MfaAuthentication;
import run.halo.app.security.authentication.mfa.MfaResponseHandler;

@Component
public class ExceptionSecurityConfigurer implements SecurityConfigurer {

    private final MfaResponseHandler mfaResponseHandler;

    public ExceptionSecurityConfigurer(MfaResponseHandler mfaResponseHandler) {
        this.mfaResponseHandler = mfaResponseHandler;
    }

    @Override
    public void configure(ServerHttpSecurity http) {
        http.exceptionHandling(exception -> {
            var mfaAccessDeniedHandler = new MfaAccessDeniedHandler();
            var mfaEntry =
                new DelegateEntry(mfaAccessDeniedHandler.getMatcher(), mfaAccessDeniedHandler);
            var accessDeniedHandler =
                new ServerWebExchangeDelegatingServerAccessDeniedHandler(mfaEntry);
            accessDeniedHandler.setDefaultAccessDeniedHandler(
                new BearerTokenServerAccessDeniedHandler());
            exception.authenticationEntryPoint(new DefaultServerAuthenticationEntryPoint())
                .accessDeniedHandler(accessDeniedHandler);
        });
    }

    private class MfaAccessDeniedHandler implements ServerAccessDeniedHandler {

        private final ServerWebExchangeMatcher matcher;

        private MfaAccessDeniedHandler() {
            matcher = exchange -> exchange.getPrincipal()
                .filter(MfaAuthentication.class::isInstance)
                .flatMap(a -> ServerWebExchangeMatcher.MatchResult.match())
                .switchIfEmpty(Mono.defer(ServerWebExchangeMatcher.MatchResult::notMatch));
        }

        @Override
        public Mono<Void> handle(ServerWebExchange exchange, AccessDeniedException denied) {
            return mfaResponseHandler.handle(exchange);
        }

        public ServerWebExchangeMatcher getMatcher() {
            return matcher;
        }
    }
}
