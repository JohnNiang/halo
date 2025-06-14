package run.halo.app.perf.adapter;

import org.springframework.stereotype.Component;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.User;
import run.halo.app.extension.Extension;
import run.halo.app.extension.JsonExtension;
import run.halo.app.extension.Metadata;
import run.halo.app.extension.Unstructured;
import run.halo.app.perf.entity.LabelEntity;
import run.halo.app.perf.entity.UserEntity;
import run.halo.app.perf.repository.LabelEntityRepository;
import run.halo.app.perf.repository.UserEntityRepository;

@Component
class UserExtensionAdapter implements ExtensionAdapter {

    private final UserEntityRepository userEntityRepository;

    private final LabelEntityRepository labelEntityRepository;

    private final ReactiveTransactionManager txManager;

    UserExtensionAdapter(UserEntityRepository userEntityRepository,
        LabelEntityRepository labelEntityRepository, ReactiveTransactionManager txManager) {
        this.userEntityRepository = userEntityRepository;
        this.labelEntityRepository = labelEntityRepository;
        this.txManager = txManager;
    }

    @Override
    public boolean support(Extension extension) {
        var gvk = extension.groupVersionKind();
        return !gvk.hasGroup() && "User".equals(gvk.kind());
    }

    @Override
    public <E extends Extension> Mono<E> create(E extension) {
        var user = asUser(extension);
        // convert user to user entity
        var userEntity = toEntity(user);
        var tx = TransactionalOperator.create(txManager);
        userEntity.markAsNew();
        return userEntityRepository.save(userEntity)
            .flatMap(created -> {
                // handle labels
                var labels = user.getMetadata().getLabels();
                if (labels == null || labels.isEmpty()) {
                    return Mono.just(created);
                }
                // create or updated labels
                return labelEntityRepository.findByEntityTypeAndEntityId("user", created.getId())
                    .collectList()
                    .flatMap(labelEntityRepository::deleteAll)
                    .then(Mono.fromSupplier(() -> labels.keySet()
                        .stream()
                        .map(labelKey -> {
                            var labelValue = labels.get(labelKey);
                            var labelEntity = new LabelEntity();
                            labelEntity.setLabelName(labelKey);
                            labelEntity.setLabelValue(labelValue);
                            labelEntity.setEntityType("user");
                            labelEntity.setEntityId(created.getId());
                            return labelEntity;
                        })
                        .toList()
                    ))
                    .flatMapMany(labelEntityRepository::saveAll)
                    .then(Mono.just(created));
            })
            .doOnNext(created -> {
                extension.getMetadata().setCreationTimestamp(created.getCreatedDate().orElse(null));
                extension.getMetadata().setVersion(created.getVersion());
            })
            .thenReturn(extension);
    }

    private UserEntity toEntity(User user) {
        var entity = new UserEntity();

        user.getMetadata().getName();
        var generateName = user.getMetadata().getGenerateName();

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
            user.getSpec().getTwoFactorAuthEnabled());
        entity.setDisabled(user.getSpec().getDisabled());
        entity.setAnnotations(user.getMetadata().getAnnotations());
        entity.setFinalizers(user.getMetadata().getFinalizers());
        return entity;
    }

    private User toUser(UserEntity entity) {
        var user = new User();
        user.setMetadata(new Metadata());
        user.setSpec(new User.UserSpec());
        user.setStatus(new User.UserStatus());

        user.getMetadata().setName(entity.getId());
        user.getMetadata().setVersion(entity.getVersion());
        // TODO ensure mutable
        user.getMetadata().setAnnotations(entity.getAnnotations());
        user.getMetadata().setFinalizers(entity.getFinalizers());
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
