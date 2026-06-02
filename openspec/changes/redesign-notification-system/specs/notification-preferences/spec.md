## ADDED Requirements

### Requirement: Get user notification preferences

The system SHALL return the notification preference matrix for the authenticated user: available categories, available notifiers, and which combinations are enabled.

#### Scenario: Get preferences matrix
- **WHEN** the authenticated user requests `GET /apis/uc.api.halo.run/v1alpha1/notification-preferences`
- **THEN** the response includes all registered categories, all registered notifiers, and a boolean matrix of enabled states

#### Scenario: Default preferences
- **WHEN** a user has never saved preferences
- **THEN** all category-notifier combinations default to enabled

### Requirement: Save user notification preferences

The system SHALL allow the authenticated user to update their notification preferences, specifying which notifiers are enabled per category.

#### Scenario: Enable email notifier for a category
- **WHEN** the user saves `{"new-comment-on-post": ["email-notifier"]}`
- **THEN** only the email notifier is enabled for that category

#### Scenario: Disable all notifiers for a category
- **WHEN** the user saves `{"new-comment-on-post": []}`
- **THEN** no notifiers are enabled for that category, and the user receives no notifications for that category

### Requirement: Preference lookup during dispatch

The system SHALL consult user preferences before dispatching a notification to a notifier. If a user has disabled a notifier for a given category, no dispatch is created for that combination.

#### Scenario: Skipped dispatch due to preference
- **WHEN** a notification is dispatched for category "new-device-login" and user has disabled all notifiers for that category
- **THEN** no dispatch record is created and no notifier is invoked
