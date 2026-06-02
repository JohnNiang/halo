CREATE
    TABLE
        IF NOT EXISTS extensions(
            name VARCHAR(255) NOT NULL,
            DATA BLOB,
            version BIGINT,
            PRIMARY KEY(name)
        );

CREATE
    TABLE
        IF NOT EXISTS notifications(
            id BIGINT AUTO_INCREMENT PRIMARY KEY,
            recipient VARCHAR(255) NOT NULL,
            category VARCHAR(100) NOT NULL,
            message_key VARCHAR(200) NOT NULL,
            message_args JSON,
            subject_url VARCHAR(1000) NOT NULL DEFAULT '',
            is_unread BOOLEAN DEFAULT TRUE,
            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
            read_at TIMESTAMP
        );

CREATE
    INDEX IF NOT EXISTS idx_notifications_recipient_unread ON
    notifications(
        recipient,
        is_unread
    );

CREATE
    INDEX IF NOT EXISTS idx_notifications_recipient_created ON
    notifications(
        recipient,
        created_at
    );

CREATE
    UNIQUE INDEX IF NOT EXISTS idx_notifications_dedup ON
    notifications(
        recipient,
        category,
        message_key,
        subject_url
    );

CREATE
    TABLE
        IF NOT EXISTS notification_dispatches(
            id BIGINT AUTO_INCREMENT PRIMARY KEY,
            notification_id BIGINT NOT NULL,
            notifier VARCHAR(100) NOT NULL,
            status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
            error_message VARCHAR(2000),
            retry_count INT DEFAULT 0,
            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
            completed_at TIMESTAMP,
            next_retry_at TIMESTAMP,
            FOREIGN KEY(notification_id) REFERENCES notifications(id) ON
            DELETE
                CASCADE
        );

CREATE
    INDEX IF NOT EXISTS idx_dispatches_status_retry ON
    notification_dispatches(
        status,
        next_retry_at
    );

CREATE
    TABLE
        IF NOT EXISTS notification_preferences(
            user_id VARCHAR(255) NOT NULL,
            category VARCHAR(100) NOT NULL,
            notifier VARCHAR(100) NOT NULL,
            enabled BOOLEAN DEFAULT TRUE,
            PRIMARY KEY(
                user_id,
                category,
                notifier
            )
        );
