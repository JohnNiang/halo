## Context

The current notification system uses 6 extension CRD types (Reason, ReasonType, Subscription, NotificationTemplate, NotifierDescriptor, Notification) stored as JSON documents in the generic extension table. The flow is: Event → Reason CRD → Reconciler (watches Reasons, adds finalizers) → NotificationCenter → Subscriber resolution (SpEL matching) → Thymeleaf template rendering → Send + Store → Delete Reason. User preferences are stored in a separate HashMap-based store (`UserNotificationPreference`), not even a proper CRD. The entire system is over-engineered for what is fundamentally "persist a notification row and optionally forward to email/SMS."

## Goals / Non-Goals

**Goals:**
- Replace the multi-hop Reason→Reconciler pipeline with direct transactional notification
- Replace CRD storage with dedicated SQL tables with proper indexes
- Simplify the domain model: remove Reason, ReasonType, Subscription, NotificationTemplate, NotifierDescriptor as CRDs
- Lightweight YAML-based category registry with plugin contribution support
- `ReactiveNotifier` as a PF4J ExtensionPoint for extensible notifier channels
- Consumer-side i18n rendering (frontend for on-site, notifier-owned for email/SMS)
- Event-driven dispatch retry (not cron polling)
- Clean `uc.api.halo.run` REST API surface

**Non-Goals:**
- Migration of existing notification data — this is a complete replacement, no migration
- Notification aggregation / digests (batch emails)
- In-app push notifications (beyond the notification center)
- Read receipts / delivery status visible to end users (dispatch tracking is internal)

## Decisions

### 1. Direct transactional notification instead of Reason CRD + Reconciler

**Decision**: Kill the entire Reason CRD and NotificationTrigger reconciler. Callers call `notificationService.notify(request)` directly within a transactional context. Persistence happens immediately; notifier dispatch fires via `@TransactionalEventListener(AFTER_COMMIT)`.

**Rationale**: The reconciler exists solely for "exactly-once" semantics and async dispatch. Exactly-once is achievable through transactional persistence + finalizer-free deduplication (the notifications table itself is the dedup source). Async dispatch is achievable through `@TransactionalEventListener` which runs after the transaction commits — no CRD, no watch loop, no finalizer lifecycle.

**Alternatives considered**: Spring events (not transactional), raw `Mono.defer().subscribeOn(Schedulers.parallel())` (fires even if transaction rolls back), message queue (new infrastructure dependency).

### 2. Dedicated SQL tables instead of extension CRD storage

**Decision**: Three new tables (`notifications`, `notification_dispatches`, `notification_preferences`) with R2DBC repositories and proper indexes.

**Rationale**: Notifications are not a plugin-extension concern — plugins need to emit notifications and register notifier channels, not extend the notification schema. SQL tables enable efficient queries on `(recipient, is_unread)`, bulk operations, and referential integrity between notifications and dispatches.

### 3. Consumer-side content rendering

**Decision**: The `notifications` table stores `message_key` + `message_args` (JSON). The frontend renders content through its i18n layer. Notifiers render their own content server-side using their own `MessageSource`.

**Rationale**: Removes the Thymeleaf template engine from the notification path entirely. No CRD-hosted templates, no `NotificationTemplateRender`, no template selection logic. The frontend already has i18n infrastructure — notifications become just another i18n consumer. Email/SMS notifiers bundle their own templates.

### 4. Lightweight YAML category registry instead of ReasonType CRDs

**Decision**: `notification-categories.yaml` resource files (`{name, displayName, description, uiPermission, hidden}`). Core categories in `application/src/main/resources/`. Plugins contribute via their own `notification-categories.yaml` in the plugin classpath, discovered through PF4J resource scanning.

**Rationale**: Categories are static metadata — name, display name, description — nothing that needs runtime CRUD. A YAML file loaded at startup is sufficient. Plugins contribute YAML files, not Java code, keeping category registration declarative.

### 5. ReactiveNotifier as PF4J ExtensionPoint

**Decision**: Single SPI:
```java
public interface ReactiveNotifier extends ExtensionPoint {
    String name();
    Mono<Void> notify(Notification notification);
    boolean supports(String recipient);
}
```
Built-in notifiers are Spring `@Component` beans, plugin notifiers are PF4J extensions. Both discovered via `ExtensionGetter.getExtensions(ReactiveNotifier.class)`.

**Rationale**: `ExtensionGetter` already merges Spring beans + PF4J extensions into a single ordered list. No need to build a separate discovery mechanism. The notifier owns rendering, so the email notifier renders HTML emails and the SMS notifier renders plain text — the core notification service doesn't need to know about rendering formats.

### 6. Event-driven retry instead of cron polling

**Decision**: Failed dispatches schedule their own retry via `Mono.delay(backoff)`. A scheduled cleanup task only handles old notification pruning (read notifications older than N days). No cron-based retry loop.

**Rationale**: Cron polling wastes cycles and introduces unnecessary latency. Event-driven retry triggers exactly when a dispatch fails, with configurable backoff and max retries. The cleanup task remains for data hygiene.

### 7. No subscription system in the notification layer

**Decision**: Callers resolve recipients in their own domain logic (e.g., comment event listener queries the post owner, reply event listener queries the replied-to user). The notification system has no concept of subscriptions, subject matching, or unsubscribe tokens.

**Rationale**: "Who should be notified about X" is domain logic, not notification infrastructure. The comment module knows who owns the post. The auth module knows who owns the device. The notification system just delivers to the recipients it's given.

### 8. Backward compatibility: keep API types, provide no-op implementations

**Decision**: Preserve all old public SPI interfaces and CRD model classes in the `api/` module. Provide no-op `@Component` implementations for `NotificationCenter` and `NotificationReasonEmitter` in the `application/` module that log deprecation warnings and return `Mono.empty()`. Do NOT restore extension YAML files — old CRDs are not registered, so the reconciler framework ignores them.

**Rationale**: Existing plugins compiled against the old notification API may autowire `NotificationCenter` or `NotificationReasonEmitter`, or reference CRD model types like `Reason`, `Subscription`, `ReasonType`, `NotificationTemplate`, `NotifierDescriptor`. Deleting these types from `api/` would cause `NoClassDefFoundError` at plugin load time. Deleting the Spring beans would cause `NoSuchBeanDefinitionException` at startup. Keeping the types (without CRD registration) and providing no-op beans means plugins boot and run cleanly — their notification calls just become silent no-ops.

**Types to keep in api/**:
- Service interfaces: `NotificationCenter`, `NotificationReasonEmitter`
- Value objects: `ReasonPayload`, `ReasonAttributes`, `UserIdentity`, `NotificationContext`
- CRD models: `Reason`, `ReasonType`, `Subscription`, `NotificationTemplate`, `NotifierDescriptor`, `Notification` (old CRD, still in tree)

**No-op beans to add in application/**:
- `DeprecatedNotificationCenter implements NotificationCenter`
- `DeprecatedNotificationReasonEmitter implements NotificationReasonEmitter`

**Things NOT restored**:
- Extension YAML (`notification.yaml`, `notification-templates.yaml`, `role-template-notification.yaml`) — no CRD registration
- Old application services (reconcilers, template renderers, senders, config stores) — stay deleted

## Risks / Trade-offs

- **[Risk] No deduplication without Reason CRD finalizers** → The notifications table needs a unique index on `(recipient, category, message_key, subject_url)` with an `ON CONFLICT DO NOTHING` or equivalent pattern. This is NOT yet implemented — the current schema only has an auto-increment PK and blind INSERTs, so duplicate events create duplicate rows. Pending: task 2.7.
- **[Risk] @TransactionalEventListener failure means notification is persisted but never dispatched** → The cleanup task picks up notifications without corresponding dispatch records and creates pending dispatches. This is a safety net, not the primary path.
- **[Risk] Retry dispatches silently fail** → `retryFailedDispatch()` loads only `notification_id` from the dispatches table and creates a bare `Notification` with null `recipient`, `category`, etc. This causes `notifier.supports(recipient)` to always reject and `notify()` to fail silently. Must JOIN the `notifications` table to hydrate the full object. Pending: task 6.6.
- **[Risk] Plugin notifiers are discovered at startup** → If a plugin registers a notifier after startup, it won't be available until restart. Acceptable for now; dynamic registration can be added later if needed.
- **[Trade-off] No built-in unsubscribe mechanism** → Without subscriptions, there's no unsubscribe token to embed in emails. Callers that want opt-out must implement it at the domain level (e.g., a user setting "don't email me about comments").
- **[Trade-off] Consumer-side rendering means email/SMS rendering is duplicated per notifier** → Each notifier bundles its own templates/MessageSource. This is intentional — an email template and an SMS template are fundamentally different formats anyway.
