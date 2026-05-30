## ADDED Requirements

### Requirement: Persist notification for each recipient

The system SHALL persist one notification record per recipient when `NotificationService.notify()` is called, each with the same category, message key, message arguments, and subject URL.

#### Scenario: Single notification with multiple recipients
- **WHEN** a caller notifies two recipients about a new comment on a post
- **THEN** two notification rows are persisted, one for each recipient, both with the same category and message data

#### Scenario: Notification persistence inside transaction
- **WHEN** the calling transaction rolls back after `notify()` is called
- **THEN** no notification rows are committed to the database

#### Scenario: Duplicate notification prevention
- **WHEN** the same event is processed twice for the same recipient, category, and message content
- **THEN** only one notification row is persisted

### Requirement: Notification contains structured i18n data

Each notification SHALL store a `message_key` and `message_args` (JSON) rather than pre-rendered content. The consumer (frontend or notifier) is responsible for rendering.

#### Scenario: Notification with structured data
- **WHEN** a notification is created for "new comment on post"
- **THEN** the record contains `message_key: "notification.new-comment-on-post.title"` and `message_args: {"commenter": "John", "postTitle": "Hello"}`

### Requirement: List user notifications with pagination

The system SHALL return paginated notifications for the authenticated user, filterable by unread status.

#### Scenario: List unread notifications
- **WHEN** the authenticated user requests `GET /apis/uc.api.halo.run/v1alpha1/notifications?unread=true&page=0&size=20`
- **THEN** the response includes up to 20 unread notifications ordered by `created_at` descending, with total count

#### Scenario: List all notifications
- **WHEN** the user requests notifications without the unread filter
- **THEN** the response includes both read and unread notifications

### Requirement: Get unread notification count

The system SHALL return the count of unread notifications for the authenticated user.

#### Scenario: Unread count
- **WHEN** the authenticated user requests `GET /apis/uc.api.halo.run/v1alpha1/notifications/unread-count`
- **THEN** the response is `{"count": N}` where N is the number of unread notifications

### Requirement: Mark single notification as read

The system SHALL allow the authenticated user to mark a single notification as read, setting `is_unread` to false and `read_at` to the current timestamp.

#### Scenario: Mark notification as read
- **WHEN** the authenticated user marks their notification with id `123` as read
- **THEN** notification `123` has `is_unread = false` and `read_at` set
- **AND** only the recipient can mark it as read

### Requirement: Batch mark notifications as read

The system SHALL allow the authenticated user to mark multiple notifications as read in a single request.

#### Scenario: Mark all notifications as read
- **WHEN** the authenticated user sends a list of notification IDs to mark as read
- **THEN** all specified notifications owned by the user are marked as read

### Requirement: Delete single notification

The system SHALL allow the authenticated user to delete a single notification they own.

#### Scenario: Delete notification
- **WHEN** the authenticated user deletes notification `123`
- **THEN** the notification is removed from the database
- **AND** associated dispatch records are also removed

### Requirement: Batch delete notifications

The system SHALL allow the authenticated user to delete multiple notifications in a single request.

#### Scenario: Batch delete notifications
- **WHEN** the authenticated user sends a list of notification IDs to delete
- **THEN** all specified notifications owned by the user are deleted, along with their dispatch records
