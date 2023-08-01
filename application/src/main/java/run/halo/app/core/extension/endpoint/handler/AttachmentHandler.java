package run.halo.app.core.extension.endpoint.handler;

import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

public interface AttachmentHandler {

    Mono<ServerResponse> uploadPasteOrGrab(ServerRequest request);

}
