package run.halo.app.perf.adapter;

import reactor.core.publisher.Mono;
import run.halo.app.extension.Extension;

public interface ExtensionAdapter {

    boolean support(Extension extension);

    <E extends Extension> Mono<E> create(E extension);

}
