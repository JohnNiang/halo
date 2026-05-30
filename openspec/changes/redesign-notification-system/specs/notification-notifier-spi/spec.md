## ADDED Requirements

### Requirement: ReactiveNotifier ExtensionPoint

The system SHALL define `ReactiveNotifier` as a PF4J ExtensionPoint with methods `name()`, `notify(Notification)`, and `supports(String recipient)`.

#### Scenario: Built-in email notifier registered
- **WHEN** the application starts
- **THEN** the email notifier is discoverable via `ExtensionGetter.getExtensions(ReactiveNotifier.class)`

#### Scenario: Plugin notifier registered
- **WHEN** a plugin implements `ReactiveNotifier`
- **THEN** the plugin's notifier is discoverable alongside built-in notifiers

### Requirement: Dispatch tracking

The system SHALL create a `notification_dispatches` record for each notifier-notification pair, with initial status `PENDING`, and update it to `SENT` or `FAILED` after the notifier processes the notification.

#### Scenario: Successful dispatch
- **WHEN** a notifier successfully sends a notification
- **THEN** the dispatch record is updated to status `SENT` with `completed_at` set

#### Scenario: Failed dispatch
- **WHEN** a notifier throws an error
- **THEN** the dispatch record is updated to status `FAILED` with the error message and a scheduled retry time

### Requirement: Notifier owns content rendering

Each `ReactiveNotifier` implementation SHALL be responsible for rendering notification content in its own format (HTML email, plain text SMS, etc.) using the notification's `message_key` and `message_args`.

#### Scenario: Email notifier renders HTML
- **WHEN** the email notifier processes a notification with `message_key: "notification.new-comment.title"`
- **THEN** it renders the email body using its own `MessageSource` and templates

### Requirement: Dispatch retry

Failed dispatches SHALL be retried with exponential backoff up to a maximum retry count. After all retries are exhausted, the dispatch SHALL remain in `FAILED` status.

#### Scenario: Retry with backoff
- **WHEN** a dispatch fails with a transient error
- **THEN** it is retried after a delay, with each subsequent retry having a longer delay
- **AND** after 3 failed retries, no further attempts are made

#### Scenario: Permanent failure
- **WHEN** a dispatch fails with a permanent error (e.g., invalid recipient address)
- **THEN** the notifier marks the dispatch as `FAILED` immediately without scheduling a retry

### Requirement: Supports check before dispatch

The system SHALL call `ReactiveNotifier.supports(recipient)` before dispatching. If it returns false, no dispatch is created for that notifier.

#### Scenario: Skip unsupported notifier
- **WHEN** a user has no email address configured
- **THEN** the email notifier's `supports()` returns false and no dispatch is created

### Requirement: @TransactionalEventListener dispatch

Notifier dispatch SHALL be triggered by `@TransactionalEventListener(phase = AFTER_COMMIT)` so that notifications are only dispatched after the notification row is committed to the database.

#### Scenario: Dispatch fires after commit
- **WHEN** a notification is persisted and the transaction commits
- **THEN** the dispatch event listener fires and processes all enabled notifiers for the notification
