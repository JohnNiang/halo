## Why

The current notification system is massively over-engineered: a 9-step pipeline (Event → Reason CRD → Reconciler with finalizers → NotificationCenter → Subscriber Resolution → Template Rendering → Send + Store → Delete Reason) built on 6 extension CRD types plus detached preference storage. This introduces latency through a reconciler watch loop, couples notifications to the extension storage layer (poor query performance, no real indexes), and forces Thymeleaf template rendering and SpEL subscription matching for what should be direct delivery. No migration is required — this is a complete replacement.

## What Changes

- **BREAKING**: Remove all notification extension CRD registrations (YAML files deleted); old CRD model classes and SPI interfaces preserved in `api/` for backward compatibility with no-op implementations in `application/`
- **BREAKING**: Replace extension-stored Notification with a dedicated SQL table (`notifications`) with proper indexes; old CRD `Notification` class retained but not registered
- **BREAKING**: Remove all current notification REST APIs and replace with new `uc.api.halo.run` endpoints
- **BREAKING**: Remove Thymeleaf template rendering; notifications store structured i18n keys + args, rendered consumer-side
- Add dedicated SQL tables for dispatch tracking (`notification_dispatches`) and user preferences (`notification_preferences`)
- Add lightweight YAML-based category registry (`notification-categories.yaml`) with plugin contribution support
- Add `ReactiveNotifier` PF4J ExtensionPoint for plugin notifier channels
- Add `@TransactionalEventListener(AFTER_COMMIT)` dispatch with event-driven retry (no cron polling)
- Rewrite frontend notification pages against new APIs with consumer-side i18n rendering
- Preserve old `NotificationCenter` and `NotificationReasonEmitter` interfaces with no-op implementations so existing plugins boot without errors

## Capabilities

### New Capabilities

- `notification-delivery`: Core notification persistence, dispatch to notifier channels, and lifecycle management (mark-as-read, delete, batch operations)
- `notification-preferences`: Per-user, per-category, per-notifier enable/disable toggles
- `notification-categories`: Lightweight registry defining available notification types with display metadata and plugin extensibility
- `notification-notifier-spi`: PF4J ExtensionPoint for notifier channels with dispatch tracking and retry

### Modified Capabilities

None — this is a greenfield replacement. No existing notification specs to modify.

## Impact

- **api/**: Remove `core/extension/notification/` models (Reason, ReasonType, Subscription, NotificationTemplate, NotifierDescriptor); add new `notification/` package with `ReactiveNotifier`, `NotificationRequest`, category model
- **application/**: Remove all current notification services, reconcilers, endpoints, templates, preference store; add new SQL-backed services, endpoints under `uc.api.halo.run`, email notifier, category YAML
- **ui/**: Rewrite UC notifications page, notification preferences page, notification list components against new APIs; remove old console notification settings (replaced by simpler per-notifier config)
- **Database**: New `notifications`, `notification_dispatches`, `notification_preferences` tables with R2DBC migrations; remove old notification-related extension data
- **Extensions YAML**: Remove `notification.yaml`, `notification-templates.yaml`, `role-template-notification.yaml`
