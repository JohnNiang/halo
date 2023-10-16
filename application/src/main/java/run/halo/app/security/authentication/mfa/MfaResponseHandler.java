package run.halo.app.security.authentication.mfa;

import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

public interface MfaResponseHandler {

    Mono<Void> handle(ServerWebExchange exchange);

}
