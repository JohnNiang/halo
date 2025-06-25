package run.halo.app.perf.adapter;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.extension.Extension;
import run.halo.app.extension.GroupVersionKind;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.ListResult;

public interface ExtensionAdapter {

    boolean support(GroupVersionKind gvk);

    <E extends Extension> Mono<E> create(E extension);

    <E extends Extension> Mono<E> update(E extension);

    <E extends Extension> Mono<E> findById(String id);

    <E extends Extension> Flux<E> findAll();

    <E extends Extension> Flux<E> findAll(ListOptions options, Sort sort);

    <E extends Extension> Mono<ListResult<E>> pageBy(ListOptions options, Pageable pageable);

    /**
     * Initialize an extension. This method is used to create or update an extension
     *
     * @param extension the extension to initialize
     * @param <E> the type of the extension
     * @return a Mono that emits the initialized extension
     */
    <E extends Extension> Mono<E> initialize(E extension);

}
