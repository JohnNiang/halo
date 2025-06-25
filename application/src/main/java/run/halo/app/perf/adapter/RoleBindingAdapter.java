package run.halo.app.perf.adapter;

import java.util.Objects;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.RoleBinding;
import run.halo.app.extension.Extension;
import run.halo.app.extension.GroupVersionKind;
import run.halo.app.extension.JsonExtension;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.ListResult;
import run.halo.app.extension.Unstructured;
import run.halo.app.perf.repository.UserRoleEntityRepository;

@Component
class RoleBindingAdapter implements ExtensionAdapter {

    private static final GroupVersionKind GVK = GroupVersionKind.fromExtension(RoleBinding.class);

    private final UserRoleEntityRepository userRoleEntityRepository;

    RoleBindingAdapter(UserRoleEntityRepository userRoleEntityRepository) {
        this.userRoleEntityRepository = userRoleEntityRepository;
    }

    @Override
    public boolean support(GroupVersionKind gvk) {
        return Objects.equals(GVK.groupKind(), gvk.groupKind());
    }

    @Override
    public <E extends Extension> Mono<E> create(E extension) {
        return Mono.error(new UnsupportedOperationException(
            "Create operation is not supported for RoleBinding: "
                + extension.getMetadata().getName()
        ));
    }

    @Override
    public <E extends Extension> Mono<E> update(E extension) {
        return Mono.error(new UnsupportedOperationException(
            "Update operation is not supported for RoleBinding: "
                + extension.getMetadata().getName()
        ));
    }

    @Override
    public <E extends Extension> Mono<E> findById(String id) {
        return Mono.error(new UnsupportedOperationException(
            "Find by ID operation is not supported for RoleBinding: " + id
        ));
    }

    @Override
    public <E extends Extension> Flux<E> findAll() {
        return Flux.error(new UnsupportedOperationException(
            "Find all operation is not supported for RoleBinding"
        ));
    }

    @Override
    public <E extends Extension> Flux<E> findAll(ListOptions options, Sort sort) {
        return Flux.error(
            new UnsupportedOperationException(
                "Find all with options and sort is not supported for RoleBinding: "
                    + options + ", " + sort
            )
        );
    }

    @Override
    public <E extends Extension> Mono<ListResult<E>> pageBy(ListOptions options,
        Pageable pageable) {
        return Mono.error(
            new UnsupportedOperationException(
                "Page by options and pageable is not supported for RoleBinding: "
                    + options + ", " + pageable
            )
        );
    }

    private RoleBinding asRoleBinding(Extension e) {
        if (e instanceof RoleBinding roleBinding) {
            return roleBinding;
        }
        if (e instanceof Unstructured unstructured) {
            return Unstructured.OBJECT_MAPPER.convertValue(unstructured, RoleBinding.class);
        }
        if (e instanceof JsonExtension jsonExtension) {
            return jsonExtension.getObjectMapper().convertValue(jsonExtension, RoleBinding.class);
        }
        throw new IllegalArgumentException("""
            Unexpected extension type: %s. Expected RoleBinding, Unstructured, or JsonExtension.\
            """.formatted(e.getClass()));
    }
}
