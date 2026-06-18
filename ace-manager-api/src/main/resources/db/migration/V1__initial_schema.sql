CREATE TABLE app_users (
    id                UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    email             VARCHAR(255) NOT NULL UNIQUE,
    password          VARCHAR(255) NOT NULL,
    role              VARCHAR(20)  NOT NULL,
    linked_entity_id  UUID
);
