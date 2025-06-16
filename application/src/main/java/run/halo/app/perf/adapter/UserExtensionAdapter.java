package run.halo.app.perf.adapter;

import org.springframework.data.domain.Sort;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.User;
import run.halo.app.extension.Extension;
import run.halo.app.extension.GroupVersionKind;
import run.halo.app.extension.JsonExtension;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.Metadata;
import run.halo.app.extension.Unstructured;
import run.halo.app.extension.router.selector.EqualityMatcher;
import run.halo.app.extension.router.selector.LabelSelector;
import run.halo.app.extension.router.selector.SetMatcher;
import run.halo.app.perf.entity.LabelEntity;
import run.halo.app.perf.entity.UserEntity;
import run.halo.app.perf.repository.LabelEntityRepository;
import run.halo.app.perf.repository.UserEntityRepository;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
class UserExtensionAdapter implements ExtensionAdapter {

    private static final GroupVersionKind GVK = GroupVersionKind.fromExtension(User.class);

    private final UserEntityRepository userEntityRepository;

    private final LabelEntityRepository labelEntityRepository;

    private final ReactiveTransactionManager txManager;

    private final R2dbcEntityTemplate entityTemplate;

    UserExtensionAdapter(UserEntityRepository userEntityRepository,
                         LabelEntityRepository labelEntityRepository,
                         ReactiveTransactionManager txManager,
                         R2dbcEntityTemplate entityTemplate) {
        this.userEntityRepository = userEntityRepository;
        this.labelEntityRepository = labelEntityRepository;
        this.txManager = txManager;
        this.entityTemplate = entityTemplate;
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
            .flatMap(created ->
                saveLabels(created.getId(), extension.getMetadata().getLabels()).thenReturn(created)
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
        var entity = toEntity(user);
        var tx = TransactionalOperator.create(txManager);
        return userEntityRepository.save(entity)
            .flatMap(updated ->
                saveLabels(updated.getId(), extension.getMetadata().getLabels()).thenReturn(updated)
            )
            .as(tx::transactional)
            .doOnNext(updated -> extension.getMetadata().setVersion(updated.getVersion()))
            .thenReturn(extension);
    }

    @Override
    public <E extends Extension> Mono<E> findById(String id) {
        return userEntityRepository.findById(id)
            .zipWith(getLabels(id), (entity, labels) -> {
                var user = toUser(entity);
                user.getMetadata().setLabels(new HashMap<>(labels));
                return (E) user;
            });
    }

    @Override
    public <E extends Extension> Flux<E> findAll() {
        return userEntityRepository.findAll()
            .flatMap(entity -> {
                var user = toUser(entity);
                return getLabels(entity.getId())
                    .doOnNext(labels -> user.getMetadata().setLabels(new HashMap<>(labels)))
                    .thenReturn((E) user);
            });
    }

    @Override
    public <E extends Extension> Flux<E> findAll(ListOptions options, Sort sort) {
        // spec.displayName
        // spec.email
        // roles
        // spec.disabled
        // build the field query
        final Criteria criteria;
        var fieldSelector = options.getFieldSelector();
        if (fieldSelector != null) {
            var fieldMap = Map.of("spec.displayName", "displayName",
                "spec.email", "email",
                "spec.disabled", "disabled");
            criteria = Criteria.empty().and(fieldSelector.query().toCriteria(fieldMap));
        } else {
            criteria = Criteria.empty();
        }

        Flux<UserEntity> queryResult;
        var matchingLabel = findEntityIdsByLabelSelector("user", options.getLabelSelector());
        if (matchingLabel != null) {
            queryResult = matchingLabel.collectList()
                .filter(ids -> !CollectionUtils.isEmpty(ids))
                .flatMapMany(ids -> {
                    var finalCriteria = criteria;
                    if (ids.size() == 1) {
                        finalCriteria = finalCriteria.and("id").is(ids.getFirst());
                    } else {
                        finalCriteria = finalCriteria.and("id").in(ids);
                    }
                    return entityTemplate.select(Query.query(finalCriteria), UserEntity.class);
                });
        } else {
            queryResult = entityTemplate.select(Query.query(criteria), UserEntity.class);
        }

        return queryResult.collectList()
            .flatMapMany(list -> {
                var ids = list.stream().map(UserEntity::getId).toList();
                return getLabels(ids)
                    .map(labelsMap -> list.stream().map(entity -> {
                        User user = toUser(entity);
                        var labels = labelsMap.get(entity.getId());
                        if (labels != null) {
                            user.getMetadata().setLabels(labels);
                        }
                        return (E) user;
                    }))
                    .flatMapMany(Flux::fromStream);
            });
    }

    @Nullable
    private Flux<String> findEntityIdsByLabelSelector(
        String entityType, LabelSelector labelSelector) {
        if (labelSelector == null) {
            return null;
        }
        var matchers = labelSelector.getMatchers();
        if (matchers == null || matchers.isEmpty()) {
            return null;
        }
        var criteria = labelSelector.getMatchers()
            .stream()
            .map(matcher -> {
                if (matcher instanceof EqualityMatcher em) {
                    switch (em.getOperator()) {
                        case EQUAL, DOUBLE_EQUAL: {
                            return Criteria.where("labelKey").is(em.getKey())
                                .and("labelValue").is(em.getValue());
                        }
                        case NOT_EQUAL: {
                            return Criteria.where("labelKey").is(em.getKey())
                                .and("labelValue").not(em.getValue());
                        }
                        default: {
                            // do nothing
                        }
                    }
                } else if (matcher instanceof SetMatcher sm) {
                    switch (sm.getOperator()) {
                        case IN: {
                            return Criteria.where("labelKey").is(sm.getKey())
                                .and("labelValue").in(List.of(sm.getValues()));
                        }
                        case NOT_IN: {
                            return Criteria.where("labelKey").is(sm.getKey())
                                .and("labelValue").notIn(List.of(sm.getValues()));
                        }
                        case EXISTS: {
                            return Criteria.where("labelKey").is(sm.getKey());
                        }
                        case NOT_EXISTS: {
                            return Criteria.where("labelKey").not(sm.getKey());
                        }
                        default: {
                        }
                    }
                }
                return null;
            })
            .filter(Objects::nonNull)
            .toList();
        if (criteria.isEmpty()) {
            return null;
        }
        var labelQuery = Query.query(Criteria.from(criteria).and("entityType").is(entityType));
        labelQuery.columns("entityId");
        return entityTemplate.select(LabelEntity.class)
            .as(String.class)
            .matching(labelQuery)
            .all();
    }

    private Mono<Map<String, String>> getLabels(String userId) {
        return labelEntityRepository.findByEntityTypeAndEntityId("user", userId)
            .collectMap(LabelEntity::getLabelName, LabelEntity::getLabelValue);
    }

    private Mono<Map<String, Map<String, String>>> getLabels(List<String> userIds) {
        if (CollectionUtils.isEmpty(userIds)) {
            return Mono.just(Map.of());
        }
        return labelEntityRepository.findByEntityTypeAndEntityIdIn("user", userIds)
            .collectMap(LabelEntity::getEntityId, labelEntity ->
                Map.of(labelEntity.getLabelName(), labelEntity.getLabelValue())
            );
    }

    private Mono<Void> saveLabels(String userId, Map<String, String> labels) {
        if (labels == null || labels.isEmpty()) {
            return Mono.empty();
        }
        return labelEntityRepository.findByEntityTypeAndEntityId("user", userId)
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
                    labelEntity.setEntityId(userId);
                    return labelEntity;
                })
                .toList()
            ))
            .flatMapMany(labelEntityRepository::saveAll)
            .then();
    }

    private UserEntity toEntity(User user) {
        var entity = new UserEntity();

        user.getMetadata().getName();

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
            Boolean.TRUE.equals(user.getSpec().getTwoFactorAuthEnabled()));
        entity.setDisabled(Boolean.TRUE.equals(user.getSpec().getDisabled()));
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
