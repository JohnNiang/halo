package run.halo.app.perf.migration;

import reactor.core.publisher.Mono;
import run.halo.app.extension.Scheme;

public interface ExtensionMigration {

    boolean support(Scheme scheme);

    Mono<Void> migrate(Scheme scheme);

}
