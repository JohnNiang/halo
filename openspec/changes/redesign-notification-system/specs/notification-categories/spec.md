## ADDED Requirements

### Requirement: Core category registry

The system SHALL load notification categories from `notification-categories.yaml` at startup. Each category defines: name (unique key), display name, description, optional UI permission, and optional hidden flag.

#### Scenario: Core categories loaded
- **WHEN** the application starts
- **THEN** categories defined in the core `notification-categories.yaml` are available for the preference matrix and validation

### Requirement: Plugin category contribution

Plugins SHALL be able to contribute notification categories by including a `notification-categories.yaml` file in their resources, merged with core categories at startup.

#### Scenario: Plugin adds a category
- **WHEN** a plugin provides `notification-categories.yaml` with a new category "plugin-event"
- **THEN** "plugin-event" appears in the preference matrix alongside core categories

### Requirement: Hidden categories

Categories marked with `hidden: true` SHALL not appear in the user preference matrix but SHALL still be valid for notification delivery.

#### Scenario: Hidden category not in preferences
- **WHEN** a category is marked as hidden
- **THEN** it is excluded from the preference matrix response
- **AND** notifications can still be created with that category

### Requirement: Category validation

The system SHALL validate that the category in a `NotificationRequest` exists in the registry. Unknown categories SHALL be rejected.

#### Scenario: Unknown category rejected
- **WHEN** a caller attempts to notify with category "nonexistent-category"
- **THEN** the request is rejected with an error
