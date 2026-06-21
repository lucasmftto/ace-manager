CREATE TABLE teachers (
    id                  UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name                VARCHAR(255) NOT NULL,
    phone               VARCHAR(20),
    email               VARCHAR(255),
    payout_model        VARCHAR(20)  NOT NULL,
    default_percentage  NUMERIC(5,2),
    default_hourly_rate NUMERIC(10,2),
    status              VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    deleted_at          TIMESTAMP
);

CREATE TABLE teacher_student_configs (
    id                   UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    teacher_id           UUID        NOT NULL REFERENCES teachers(id),
    student_id           UUID        NOT NULL REFERENCES students(id),
    override_percentage  NUMERIC(5,2),
    override_hourly_rate NUMERIC(10,2),
    created_at           TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP   NOT NULL DEFAULT NOW(),
    deleted_at           TIMESTAMP,
    UNIQUE (teacher_id, student_id)
);
