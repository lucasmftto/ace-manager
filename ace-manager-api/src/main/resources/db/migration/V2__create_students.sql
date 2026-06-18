CREATE TABLE students (
    id                        UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name                      VARCHAR(255) NOT NULL,
    phone                     VARCHAR(20),
    email                     VARCHAR(255),
    birth_date                DATE,
    guardian_name             VARCHAR(255),
    guardian_phone            VARCHAR(20),
    agreed_monthly_value      NUMERIC(10,2),
    current_monthly_value     NUMERIC(10,2),
    preferred_payment_method  VARCHAR(20)  NOT NULL DEFAULT 'PIX',
    status                    VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    notes                     TEXT,
    created_at                TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at                TIMESTAMP    NOT NULL DEFAULT NOW(),
    deleted_at                TIMESTAMP
);

CREATE INDEX idx_students_status ON students (status) WHERE deleted_at IS NULL;
CREATE INDEX idx_students_name   ON students (name)   WHERE deleted_at IS NULL;
