CREATE TABLE class_schedules (
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name             VARCHAR(255) NOT NULL,
    day_of_week      VARCHAR(10)  NOT NULL,
    start_time       TIME         NOT NULL,
    duration_minutes INTEGER      NOT NULL,
    teacher_id       UUID         NOT NULL REFERENCES teachers(id),
    type             VARCHAR(20)  NOT NULL,
    max_students     INTEGER      NOT NULL DEFAULT 1,
    status           VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at       TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP    NOT NULL DEFAULT NOW(),
    deleted_at       TIMESTAMP
);

CREATE TABLE class_schedule_students (
    id                UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
    class_schedule_id UUID      NOT NULL REFERENCES class_schedules(id),
    student_id        UUID      NOT NULL REFERENCES students(id),
    student_plan_id   UUID      REFERENCES student_plans(id),
    created_at        TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted_at        TIMESTAMP,
    UNIQUE (class_schedule_id, student_id)
);

CREATE TABLE class_occurrences (
    id                UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
    class_schedule_id UUID      NOT NULL REFERENCES class_schedules(id),
    occurrence_date   DATE      NOT NULL,
    teacher_id        UUID      NOT NULL REFERENCES teachers(id),
    status            VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    created_at        TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted_at        TIMESTAMP,
    UNIQUE (class_schedule_id, occurrence_date)
);

CREATE TABLE attendance_records (
    id                   UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    class_occurrence_id  UUID         NOT NULL REFERENCES class_occurrences(id),
    student_id           UUID         NOT NULL REFERENCES students(id),
    status               VARCHAR(30)  NOT NULL DEFAULT 'PRESENT',
    student_billed_value NUMERIC(10,2),
    teacher_payout_value NUMERIC(10,2),
    created_at           TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP    NOT NULL DEFAULT NOW(),
    deleted_at           TIMESTAMP,
    UNIQUE (class_occurrence_id, student_id)
);

CREATE INDEX idx_occurrences_date      ON class_occurrences (occurrence_date);
CREATE INDEX idx_occurrences_teacher   ON class_occurrences (teacher_id, occurrence_date);
CREATE INDEX idx_attendance_student    ON attendance_records (student_id);
CREATE INDEX idx_attendance_occurrence ON attendance_records (class_occurrence_id);
