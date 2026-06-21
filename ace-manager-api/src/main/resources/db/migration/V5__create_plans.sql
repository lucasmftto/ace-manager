CREATE TABLE plans (
    id                   UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    name                 VARCHAR(255)  NOT NULL,
    description          TEXT,
    type                 VARCHAR(20)   NOT NULL,
    reference_price      NUMERIC(10,2) NOT NULL,
    weekly_class_count   INTEGER,
    billing_day_of_month INTEGER,
    total_classes        INTEGER,
    max_students         INTEGER,
    status               VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    created_at           TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP     NOT NULL DEFAULT NOW(),
    deleted_at           TIMESTAMP
);

CREATE TABLE student_plans (
    id                UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id        UUID          NOT NULL REFERENCES students(id),
    plan_id           UUID          NOT NULL REFERENCES plans(id),
    teacher_id        UUID          REFERENCES teachers(id),
    billed_value      NUMERIC(10,2) NOT NULL,
    start_date        DATE,
    end_date          DATE,
    remaining_classes INTEGER,
    status            VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    created_at        TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP     NOT NULL DEFAULT NOW(),
    deleted_at        TIMESTAMP
);

CREATE INDEX idx_student_plans_student ON student_plans (student_id) WHERE status = 'ACTIVE';
CREATE INDEX idx_student_plans_plan    ON student_plans (plan_id);
