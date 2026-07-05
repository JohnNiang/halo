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
        IF NOT EXISTS users(
            name VARCHAR(255) NOT NULL,
            display_name VARCHAR(255),
            email VARCHAR(255),
            email_verified BOOLEAN NOT NULL DEFAULT FALSE,
            disabled BOOLEAN NOT NULL DEFAULT FALSE,
            creation_timestamp TIMESTAMP NOT NULL,
            deletion_timestamp TIMESTAMP,
            version BIGINT,
            PRIMARY KEY(name)
        );

CREATE
    INDEX IF NOT EXISTS idx_user_email ON
    users(email);

CREATE
    INDEX IF NOT EXISTS idx_user_email_verified ON
    users(email_verified);

CREATE
    INDEX IF NOT EXISTS idx_user_disabled ON
    users(disabled);

CREATE
    INDEX IF NOT EXISTS idx_user_display_name ON
    users(display_name);

CREATE
    INDEX IF NOT EXISTS idx_user_creation_ts ON
    users(creation_timestamp);

CREATE
    INDEX IF NOT EXISTS idx_user_deletion_ts ON
    users(deletion_timestamp);

CREATE
    TABLE
        IF NOT EXISTS extension_labels(
            extension_name VARCHAR(255) NOT NULL,
            label_key VARCHAR(255) NOT NULL,
            label_value VARCHAR(255) NOT NULL,
            PRIMARY KEY(
                extension_name,
                label_key
            )
        );

CREATE
    INDEX IF NOT EXISTS idx_label_kv ON
    extension_labels(
        label_key,
        label_value
    );

CREATE
    TABLE
        IF NOT EXISTS user_roles(
            store_name VARCHAR(255) NOT NULL,
            role_name VARCHAR(255) NOT NULL,
            PRIMARY KEY(
                store_name,
                role_name
            )
        );

CREATE
    INDEX IF NOT EXISTS idx_user_role_name ON
    user_roles(role_name);
