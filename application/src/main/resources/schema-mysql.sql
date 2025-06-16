create table if not exists extensions
(
    name    varchar(255) not null COLLATE utf8mb4_bin,
    data    longblob,
    version bigint,
    primary key (name)
);

create table if not exists users (
    id varchar(255) not null,
    display_name varchar(255) null,
    avatar varchar(255) null,
    bio varchar(255) null,
    email varchar(255) not null,
    email_verified boolean default false not null,
    phone varchar(255) null,
    encoded_password varchar(255) not null,
    two_factor_auth_enabled boolean default false not null,
    totp_encrypted_secret varchar(255) null,
    disabled boolean default false not null,
    finalizers json null,
    annotations json null,
    created_by varchar(255) not null,
    last_modified_by varchar(255) not null,
    deleted_date timestamp null,
    created_date timestamp default current_timestamp not null,
    last_modified_date timestamp default current_timestamp not null,
    version bigint default 0 not null,
    primary key (id),
    index idx_email(email),
    index idx_phone(phone)
);

create table if not exists labels (
    id bigint auto_increment not null,
    entity_type varchar(255) not null,
    entity_id varchar(255) not null,
    label_name varchar(255) not null,
    label_value varchar(1023) not null,
    primary key (id),
    UNIQUE (entity_type, entity_id, label_name),
    INDEX idx_entity(entity_type, entity_id),
)
