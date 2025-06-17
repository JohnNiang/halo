create table if not exists extensions
(
    name    varchar(255) not null,
    data    blob,
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
    encoded_password varchar(255) null,
    two_factor_auth_enabled boolean default false not null,
    totp_encrypted_secret varchar(255) null,
    disabled boolean default false not null,
    finalizers varchar(1023) null,
    annotations varchar(4095) null,
    created_by varchar(255) not null,
    last_modified_by varchar(255) not null,
    deleted_date timestamp with time zone null,
    created_date timestamp with time zone default current_timestamp not null,
    last_modified_date timestamp with time zone default current_timestamp not null,
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
    index idx_entity(entity_type, entity_id)
);

create table if not exists roles (
    id varchar(255) not null,
    display_name varchar(255) not null,
    description varchar(1023) null,
    reserved boolean default false not null,
    created_by varchar(255) not null,
    last_modified_by varchar(255) not null,
    deleted_date timestamp with time zone null,
    created_date timestamp with time zone default current_timestamp not null,
    last_modified_date timestamp with time zone default current_timestamp not null,
    annotations varchar(4095) null,
    version bigint default 0 not null,

    primary key(id)
);

create table if not exists permissions (
    id varchar(255) not null,
    display_name varchar(255) not null,
    description varchar(1023) null,
    category varchar(255) null,
    ui_permissions varchar(1023) null, -- JSON array of UI permissions
    rules varchar(2043) null, -- JSON array of rules
    dependencies varchar(1023) null, -- JSON array of permission IDs that this permission depends on
    created_date timestamp with time zone default current_timestamp not null,
    deleted_date timestamp with time zone null,
    annotations varchar(4095) null,
    version bigint default 0 not null,

    primary key(id)
);

create table if not exists user_roles (
    id bigint auto_increment not null,
    user_id varchar(255) not null,
    role_id varchar(255) not null,
    created_date timestamp with time zone default current_timestamp not null,
    deleted_date timestamp with time zone null,
    version bigint default 0 not null,

    primary key (id),
    unique (user_id, role_id)
);

create table if not exists role_permissions (
    id bigint auto_increment not null,
    role_id varchar(255) not null,
    permission_id varchar(255) not null,
    created_date timestamp with time zone default current_timestamp not null,
    deleted_date timestamp with time zone null,
    version bigint default 0 not null,

    primary key (id),
    unique (role_id, permission_id)
);
