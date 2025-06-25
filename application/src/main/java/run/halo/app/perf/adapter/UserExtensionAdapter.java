package run.halo.app.perf.adapter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.sql.Select;
import org.springframework.r2dbc.core.PreparedOperation;
import org.springframework.stereotype.Component;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.User;
import run.halo.app.extension.Extension;
import run.halo.app.extension.GroupVersionKind;
import run.halo.app.extension.JsonExtension;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.ListResult;
import run.halo.app.extension.Metadata;
import run.halo.app.extension.Unstructured;
import run.halo.app.extension.exception.ExtensionNotFoundException;
import run.halo.app.perf.entity.UserEntity;
import run.halo.app.perf.repository.UserEntityRepository;
import run.halo.app.perf.service.LabelService;
import run.halo.app.perf.util.QueryUtils;

@Component
class UserExtensionAdapter implements ExtensionAdapter {

    private static final GroupVersionKind GVK = GroupVersionKind.fromExtension(User.class);

    private final UserEntityRepository userEntityRepository;

    private final ReactiveTransactionManager txManager;

    private final R2dbcEntityTemplate entityTemplate;

    private final LabelService labelService;

    private static final Map<String, String> FIELD_MAP = Map.of(
        "spec.displayName", "display_name",
        "spec.email", "email",
        "spec.disabled", "disabled",
        "metadata.name", "id",
        "metadata.creationTimestamp", "created_date",
        "metadata.deletionTimestamp", "deleted_date"
    );

    UserExtensionAdapter(UserEntityRepository userEntityRepository,
        ReactiveTransactionManager txManager,
        R2dbcEntityTemplate entityTemplate, LabelService labelService) {
        this.userEntityRepository = userEntityRepository;
        this.txManager = txManager;
        this.entityTemplate = entityTemplate;
        this.labelService = labelService;
    }

    @Override
    public boolean support(GroupVersionKind gvk) {
        return Objects.equals(GVK.groupKind(), gvk.groupKind());
    }

    @Override
    public <E extends Extension> Mono<E> create(E extension) {
        var user = asUser(extension);
        // convert user to user entity
        var userEntity = toEntity(user);
        var tx = TransactionalOperator.create(txManager);
        userEntity.markAsNew();
        return userEntityRepository.save(userEntity)
            .flatMap(created -> labelService.saveLabels(
                        GVK.groupKind(), created.getId(), extension.getMetadata().getLabels()
                    )
                    .thenReturn(created)
            )
            .as(tx::transactional)
            .doOnNext(created -> {
                extension.getMetadata().setCreationTimestamp(created.getCreatedDate().orElse(null));
                extension.getMetadata().setVersion(created.getVersion());
            })
            .thenReturn(extension);
    }

    @Override
    public <E extends Extension> Mono<E> update(E extension) {
        var user = asUser(extension);
        var tx = TransactionalOperator.create(txManager);
        return userEntityRepository.findById(extension.getMetadata().getName())
            .switchIfEmpty(Mono.error(() ->
                new ExtensionNotFoundException(GVK, extension.getMetadata().getName())
            ))
            .flatMap(entity -> {
                // update entity
                updateEntity(entity, user);
                return userEntityRepository.save(entity);
            })
            .flatMap(updated -> labelService.saveLabels(
                        GVK.groupKind(), updated.getId(), extension.getMetadata().getLabels()
                    )
                    .thenReturn(updated)
            )
            .as(tx::transactional)
            .doOnNext(updated -> extension.getMetadata().setVersion(updated.getVersion()))
            .thenReturn(extension);
    }

    @Override
    public <E extends Extension> Mono<E> findById(String id) {
        return userEntityRepository.findById(id)
            .zipWith(labelService.getLabels(GVK.groupKind(), id), (entity, labels) -> {
                var user = toUser(entity);
                user.getMetadata().setLabels(new HashMap<>(labels));
                return (E) user;
            });
    }

    @Override
    public <E extends Extension> Flux<E> findAll() {
        return findAll(new ListOptions(), Sort.unsorted());
    }

    @Override
    public <E extends Extension> Flux<E> findAll(ListOptions options, Sort sort) {
        var queryResult = QueryUtils.findAllBy(
            entityTemplate, options, sort, FIELD_MAP, GVK.groupKind(), UserEntity.class
        );

        return queryResult.collectList()
            .flatMapMany(list -> {
                var ids = list.stream().map(UserEntity::getId).collect(Collectors.toSet());
                return labelService.getLabels(GVK.groupKind(), ids)
                    .map(labelsMap -> list.stream().map(entity -> {
                        var user = toUser(entity);
                        var labels = labelsMap.get(entity.getId());
                        if (labels != null) {
                            user.getMetadata().setLabels(labels);
                        }
                        return (E) user;
                    }))
                    .flatMapMany(Flux::fromStream);
            });
    }

    private PreparedOperation<Select> prepareFind(ListOptions listOptions, Pageable pageable) {
        return QueryUtils.prepareSelect(
            entityTemplate, listOptions, pageable, FIELD_MAP, GVK.groupKind(), UserEntity.class
        );
    }

    @Override
    public <E extends Extension> Mono<ListResult<E>> pageBy(
        ListOptions options, Pageable pageable
    ) {
        var queryResult = QueryUtils.pageBy(
            entityTemplate, options, pageable, FIELD_MAP, UserEntity.GK, UserEntity.class
        );
        return queryResult.flatMap(page -> {
            var ids = page.stream().map(UserEntity::getId).toList();
            return labelService.getLabels(GVK.groupKind(), ids)
                .map(labelsMap -> page.map(entity -> {
                    var user = toUser(entity);
                    var labels = labelsMap.get(entity.getId());
                    if (labels != null) {
                        user.getMetadata().setLabels(new HashMap<>(labels));
                    }
                    return (E) user;
                }))
                .map(p -> new ListResult<>(
                    p.getNumber(), p.getSize(), p.getTotalElements(), p.getContent()
                ));
        });
    }

    @Override
    public <E extends Extension> Mono<E> initialize(E extension) {
        return update(extension)
            .onErrorResume(ExtensionNotFoundException.class, e -> create(extension));
    }

    private void updateEntity(UserEntity entity, User user) {
        entity.setId(user.getMetadata().getName());
        entity.setEmail(user.getSpec().getEmail());
        entity.setDisplayName(user.getSpec().getDisplayName());
        entity.setAvatar(user.getSpec().getAvatar());
        entity.setBio(user.getSpec().getBio());
        entity.setPhone(user.getSpec().getPhone());
        entity.setEncodedPassword(user.getSpec().getPassword());
        entity.setEmailVerified(user.getSpec().isEmailVerified());
        entity.setTotpEncryptedSecret(user.getSpec().getTotpEncryptedSecret());
        entity.setTwoFactorAuthEnabled(
            Boolean.TRUE.equals(user.getSpec().getTwoFactorAuthEnabled())
        );
        entity.setDisabled(Boolean.TRUE.equals(user.getSpec().getDisabled()));
        entity.setAnnotations(user.getMetadata().getAnnotations());
        entity.setFinalizers(user.getMetadata().getFinalizers());
        if (user.getMetadata().getVersion() != null) {
            entity.setVersion(user.getMetadata().getVersion());
        }
        if (user.getMetadata().getCreationTimestamp() != null) {
            entity.setCreatedDate(user.getMetadata().getCreationTimestamp());
        }
        entity.setDeletedDate(user.getMetadata().getDeletionTimestamp());
    }

    private UserEntity toEntity(User user) {
        var entity = new UserEntity();
        updateEntity(entity, user);
        return entity;
    }

    private User toUser(UserEntity entity) {
        var user = new User();
        user.setMetadata(new Metadata());
        user.setSpec(new User.UserSpec());
        user.setStatus(new User.UserStatus());

        user.getMetadata().setName(entity.getId());
        user.getMetadata().setVersion(entity.getVersion());
        if (entity.getAnnotations() != null) {
            user.getMetadata().setAnnotations(new HashMap<>(entity.getAnnotations()));
        }
        if (entity.getFinalizers() != null) {
            user.getMetadata().setFinalizers(new HashSet<>(entity.getFinalizers()));
        }
        user.getMetadata().setCreationTimestamp(entity.getCreatedDate().orElse(null));
        user.getMetadata().setDeletionTimestamp(entity.getDeletedDate());

        user.getSpec().setEmail(entity.getEmail());
        user.getSpec().setDisplayName(entity.getDisplayName());
        user.getSpec().setAvatar(entity.getAvatar());
        user.getSpec().setBio(entity.getBio());
        user.getSpec().setPhone(entity.getPhone());
        user.getSpec().setPassword(entity.getEncodedPassword());
        user.getSpec().setEmailVerified(entity.isEmailVerified());
        user.getSpec().setTotpEncryptedSecret(entity.getTotpEncryptedSecret());
        user.getSpec().setTwoFactorAuthEnabled(entity.isTwoFactorAuthEnabled());
        user.getSpec().setDisabled(entity.isDisabled());

        // TODO Handle status
        return user;
    }

    private User asUser(Extension extension) {
        if (extension instanceof User user) {
            return user;
        }
        if (extension instanceof Unstructured) {
            return Unstructured.OBJECT_MAPPER.convertValue(extension, User.class);
        } else if (extension instanceof JsonExtension jsonExtension) {
            return jsonExtension.getObjectMapper().convertValue(jsonExtension, User.class);
        } else {
            throw new IllegalArgumentException(
                "Unsupported extension type: " + extension.getClass());
        }
    }

}
