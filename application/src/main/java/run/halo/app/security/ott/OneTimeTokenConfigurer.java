package run.halo.app.security.ott;

import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.ott.OneTimeToken;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.authentication.ott.ServerOneTimeTokenGenerationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import run.halo.app.security.authentication.SecurityConfigurer;

@Component
class OneTimeTokenConfigurer implements SecurityConfigurer {

    @Override
    public void configure(ServerHttpSecurity http) {
        http.oneTimeTokenLogin(spec -> {
        });
    }

    @Bean
    ServerOneTimeTokenGenerationSuccessHandler oneTimeTokenGenerationSuccessHandler() {
        return new ServerOneTimeTokenGenerationSuccessHandler() {
            @Override
            public Mono<Void> handle(ServerWebExchange exchange, OneTimeToken oneTimeToken) {
                // Custom logic for handling the generation of one-time tokens
                return null;
            }
        };
    }
}
