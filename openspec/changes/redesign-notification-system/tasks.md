## 1. Tear down old notification system (with backward compatibility)

- [x] 1.1 Keep old extension CRD models in `api/src/main/java/run/halo/app/core/extension/notification/` for backward compatibility (Reason, ReasonType, Subscription, NotificationTemplate, NotifierDescriptor, Notification). Do NOT delete these.
- [x] 1.2 Delete old extension YAML: `notification.yaml`, `notification-templates.yaml`, `role-template-notification.yaml` from `application/src/main/resources/extensions/` (CRDs are not registered, but Java classes remain)
- [x] 1.3 Delete old notification services: NotificationTrigger, DefaultNotificationCenter, DefaultNotificationSender, DefaultNotificationReasonEmitter, SubscriptionServiceImpl, RecipientResolverImpl, ReasonNotificationTemplateSelectorImpl, DefaultNotificationTemplateRender, NotificationAutoCleanupTask
- [x] 1.4 Delete old notification endpoints: UserNotificationEndpoint, UserNotificationPreferencesEndpoint, SubscriptionRouter
- [x] 1.5 Restore old notification interfaces to `api/` for backward compatibility: NotificationCenter, NotificationReasonEmitter, ReasonPayload, ReasonAttributes, UserIdentity, NotificationContext (keep types, delete implementations only)
- [x] 1.6 Delete old application interfaces: UserNotificationService, NotificationSender, SubscriptionService, RecipientResolver, NotifierConfigStore, UserNotificationPreferenceService, ReasonNotificationTemplateSelector, NotificationTemplateRender
- [x] 1.7 Delete UserNotificationPreference model and NotificationProperties
- [x] 1.8 Delete old frontend notification components and pages in `ui/uc-src/modules/notifications/` and `ui/uc-src/modules/profile/tabs/NotificationPreferences.vue`
- [x] 1.9 Delete old console notification settings: `ui/console-src/modules/system/settings/tabs/Notifications.vue` and `NotificationSetting.vue`
- [x] 1.10 Delete old frontend dashboard notification widget: `ui/console-src/modules/dashboard/widgets/presets/users/NotificationWidget.vue`
- [x] 1.11 Verify project compiles after deletions (`./gradlew :api:compileJava :application:compileJava`)
- [x] 1.12 Create no-op `NotificationCenter` implementation (`DeprecatedNotificationCenter`) — logs deprecation warning, returns `Mono.empty()` for all methods
- [x] 1.13 Create no-op `NotificationReasonEmitter` implementation (`DeprecatedNotificationReasonEmitter`) — logs deprecation warning, returns `Mono.empty()`
- [x] 1.14 Remove stale notification classes: `SubscriberEmailResolver.java`, `DefaultSubscriberEmailResolver.java` (empty stubs), `LanguageUtils.java` (dead code, only used by old Thymeleaf rendering). Also delete old `Notification.java` CRD from `api/src/main/java/run/halo/app/core/extension/notification/` since it has zero references and the new POJO lives at `api/src/main/java/run/halo/app/notification/Notification.java`

## 2. New data model and migrations

- [x] 2.1 Create R2DBC migration for `notifications` table with indexes on `(recipient, is_unread)` and `(recipient, created_at)`
- [x] 2.2 Create R2DBC migration for `notification_dispatches` table with index on `(status, next_retry_at)`
- [x] 2.3 Create R2DBC migration for `notification_preferences` table with composite unique constraint on `(user_id, category, notifier)`
- [x] 2.4 Create Spring Data R2DBC repository for `notifications` with custom queries (paginated list by recipient, unread filter, unread count, batch mark-as-read, batch delete)
- [x] 2.5 Create Spring Data R2DBC repository for `notification_dispatches` with queries for pending retries
- [x] 2.6 Create Spring Data R2DBC repository for `notification_preferences` with queries for user preferences by category
- [x] 2.7 Add deduplication: unique index on `notifications(recipient, category, message_key, subject_url)` across all 4 schemas; update `DefaultNotificationService.notify()` to handle duplicate-insert gracefully (e.g., `INSERT ... ON CONFLICT DO NOTHING` or query-before-insert)

## 3. API module — new interfaces and models

- [x] 3.1 Create `ReactiveNotifier` ExtensionPoint interface in `api/src/main/java/run/halo/app/notification/`
- [x] 3.2 Create `NotificationRequest` record (recipients, category, messageKey, messageArgs, subjectUrl)
- [x] 3.3 Create `Notification` domain class (POJO, not an extension)
- [x] 3.4 Create `NotificationCategory` record (name, displayName, description, uiPermission, hidden)
- [x] 3.5 Create `NotificationService` interface with `notify(NotificationRequest)` method
- [x] 3.6 Create `NotificationPreferenceService` interface (get, save)
- [x] 3.7 Create `NotificationCategoryRegistry` interface (getCategories, getCategory, exists)

## 4. Core notification service implementation

- [x] 4.1 Implement `DefaultNotificationService.notify()` — validate category, insert one row per recipient
- [x] 4.2 Implement duplicate prevention (unique constraint or query-before-insert)
- [x] 4.3 Implement `DefaultNotificationService` CRUD: list (paginated, with unread filter), get unread count, mark single as read, mark batch as read, delete single, delete batch
- [x] 4.4 Publish a `NotificationPersistedEvent` per notification for the dispatch listener to consume
- [x] 4.5 Write unit tests for `DefaultNotificationService`

## 5. Category registry implementation

- [x] 5.1 Create `notification-categories.yaml` in `application/src/main/resources/` with initial categories: new-comment-on-post, new-comment-on-single-page, someone-replied-to-you, email-verification, reset-password-by-email, new-device-login
- [x] 5.2 Implement `DefaultNotificationCategoryRegistry` loading core YAML at startup
- [x] 5.3 Implement plugin category discovery via PF4J resource scanning (merge plugin YAMLs)
- [x] 5.4 Write unit tests for category registry

## 6. Notifier SPI and dispatch

- [x] 6.1 Implement `DefaultNotificationDispatchService` — `@TransactionalEventListener(AFTER_COMMIT)` consuming `NotificationPersistedEvent`
- [x] 6.2 Implement dispatch logic: query preferences, create dispatch records (PENDING), call `ReactiveNotifier.notify()`, update to SENT/FAILED
- [x] 6.3 Implement retry logic: on FAILED, schedule `Mono.delay(backoff)` and re-attempt up to 3 times
- [x] 6.4 Wire `ExtensionGetter` to discover all `ReactiveNotifier` beans (Spring + PF4J)
- [x] 6.5 Write unit tests for dispatch and retry logic
- [x] 6.6 Fix retry path: `retryFailedDispatch()` only sets `notification.id` — it must JOIN the `notifications` table to hydrate `recipient`, `category`, `messageKey`, `messageArgs`, `subjectUrl` so that `notifier.supports()` and `notifier.notify()` receive a complete `Notification` object

## 7. Email notifier implementation

- [x] 7.1 Implement `EmailNotifier` as a Spring `@Component` implementing `ReactiveNotifier`
- [x] 7.2 Bundle email templates (Thymeleaf or plain `MessageSource`) for each category under `email-notifier/`
- [x] 7.3 Implement `supports()` — check if user has an email address configured
- [x] 7.4 Wire SMTP configuration from system settings (remove old `NotifierDescriptor.SenderSettingRef` indirection)
- [x] 7.5 Write unit tests for email notifier
- [x] 7.6 Fix `EmailNotifier.supports()`: `blockOptional()` blocks the reactive chain — replace with a non-blocking approach or make `ReactiveNotifier.supports()` return `Mono<Boolean>`

## 8. REST API endpoints

- [x] 8.1 Create `UserNotificationEndpoint` router under `uc.api.halo.run/v1alpha1` with GET/PUT/DELETE routes
- [x] 8.2 Extract authenticated username from security context (no userspaces path parameter)
- [x] 8.3 Create `UserNotificationPreferenceEndpoint` router for GET/PUT preferences
- [x] 8.4 Add OpenAPI annotations to all new endpoints
- [x] 8.5 Run `./gradlew generateOpenApiDocs` and verify generated spec
- [x] 8.6 Write integration tests for all endpoints

## 9. Scheduled cleanup task

- [x] 9.1 Implement `NotificationCleanupTask` — cron-driven, deletes read notifications older than configured retention period
- [x] 9.2 Configure cleanup properties (`halo.notification.cleanup.enabled`, `cleanup.retention-days`, `cleanup.cron`)
- [x] 9.3 Write unit tests for cleanup task

## 10. Frontend — API client regeneration

- [x] 10.1 Run `pnpm -C ui api-client:gen` to regenerate TypeScript API client from new OpenAPI specs
- [x] 10.2 Verify new notification API types and methods are generated correctly

## 11. Frontend — notification pages

- [x] 11.1 Create notification list page with unread/read tabs, pagination, and unread count badge
- [x] 11.2 Create notification item component displaying category icon, title (rendered via i18n), content preview, relative timestamp
- [x] 11.3 Create notification detail/content view (full message rendered via i18n `$t(messageKey, messageArgs)`)
- [x] 11.4 Implement mark-as-read (single + batch "mark all as read")
- [x] 11.5 Implement delete (single + batch selection with Shift/Ctrl multi-select)
- [x] 11.6 Add i18n keys for all notification categories and message templates

## 12. Frontend — notification preferences page

- [x] 12.1 Create preferences page rendering the category × notifier matrix (rows = categories, columns = notifiers)
- [x] 12.2 Implement toggle switches to enable/disable notifiers per category
- [x] 12.3 Wire preferences API calls with optimistic UI updates
- [x] 12.4 Hide categories marked as `hidden: true` from the preference matrix

## 13. Wire up existing notification callers

- [x] 13.1 Rewrite `CommentNotificationReasonPublisher` to call `notificationService.notify()` directly with resolved recipients (no Reason creation)
- [x] 13.2 Rewrite `ReplyNotificationSubscriptionHelper` — replace subscription auto-creation with direct recipient resolution (deleted entirely)
- [x] 13.3 Rewrite `NewDeviceLoginListener` to call `notificationService.notify()` directly
- [x] 13.4 Rewrite `EmailVerificationServiceImpl` notification emission to use `NotificationService`
- [x] 13.5 Rewrite `EmailPasswordRecoveryServiceImpl` notification emission to use `NotificationService`
- [x] 13.6 Verify all callers compile and pass their existing tests (or update tests)

## 14. Final validation

- [x] 14.1 Run `./gradlew spotlessApply` for backend formatting
- [x] 14.2 Run `./gradlew :application:test` for all backend tests
- [x] 14.3 Run `pnpm -C ui typecheck && pnpm -C ui lint` for frontend validation
- [x] 14.4 Run `pnpm -C ui test:unit` for frontend unit tests
- [x] 14.5 Build full project with `./gradlew build` to verify packaging
