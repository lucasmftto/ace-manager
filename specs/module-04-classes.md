# Module: Class Management

> Extends `CLAUDE.md`. All base rules apply.

## Overview

The **central module** of Ace Manager. Manages the recurring weekly schedule
of every class, tracks attendance per occurrence, and generates the data that
feeds both Student Financials and Teacher Payouts. Only confirmed attendances
generate teacher payout entries.

---

## Domain Model

### Entity: `ClassSchedule`

Defines a recurring weekly class slot (the "template").

```java
@Entity
@Table(name = "class_schedules")
public class ClassSchedule extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;               // e.g. "Abel - Individual - Tuesday 07h"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DayOfWeek dayOfWeek;       // MONDAY ... SUNDAY

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private Integer durationMinutes;   // e.g. 60

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClassType type;            // INDIVIDUAL or GROUP

    @Column(nullable = false)
    private Integer maxStudents;       // 1 for INDIVIDUAL, N for GROUP

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClassScheduleStatus status;

    @OneToMany(mappedBy = "classSchedule")
    private List<ClassScheduleStudent> enrolledStudents;
}
```

### Entity: `ClassScheduleStudent`

Links students to a recurring class (the schedule roster).

```java
@Entity
@Table(name = "class_schedule_students",
       uniqueConstraints = @UniqueConstraint(columnNames = {"class_schedule_id", "student_id"}))
public class ClassScheduleStudent extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_schedule_id", nullable = false)
    private ClassSchedule classSchedule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_plan_id")
    private StudentPlan studentPlan;    // which plan covers this class
}
```

### Entity: `ClassOccurrence`

One actual instance of a recurring class on a specific date.

```java
@Entity
@Table(name = "class_occurrences")
public class ClassOccurrence extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_schedule_id", nullable = false)
    private ClassSchedule classSchedule;

    @Column(nullable = false)
    private LocalDate occurrenceDate;

    // Teacher may differ from schedule (substitution)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OccurrenceStatus status;   // SCHEDULED, COMPLETED, CANCELLED

    @OneToMany(mappedBy = "classOccurrence", cascade = CascadeType.ALL)
    private List<AttendanceRecord> attendanceRecords;
}
```

### Entity: `AttendanceRecord`

Tracks each student's attendance for a specific occurrence.

```java
@Entity
@Table(name = "attendance_records",
       uniqueConstraints = @UniqueConstraint(columnNames = {"class_occurrence_id", "student_id"}))
public class AttendanceRecord extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_occurrence_id", nullable = false)
    private ClassOccurrence classOccurrence;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttendanceStatus status;   // PRESENT, ABSENT, JUSTIFIED_ABSENCE

    // Computed values stored for payout calculation (denormalized for performance)
    @Column(precision = 10, scale = 2)
    private BigDecimal studentBilledValue;  // value at time of class

    @Column(precision = 10, scale = 2)
    private BigDecimal teacherPayoutValue;  // computed payout for this class
}
```

### Enums

```java
public enum ClassType { INDIVIDUAL, GROUP }
public enum ClassScheduleStatus { ACTIVE, INACTIVE }
public enum OccurrenceStatus { SCHEDULED, COMPLETED, CANCELLED }
public enum AttendanceStatus { PRESENT, ABSENT, JUSTIFIED_ABSENCE }
```

### Business rules

- `ClassOccurrence` rows are generated automatically for a date range
  (e.g., generate the next 4 weeks from today) based on `ClassSchedule`
- Only `PRESENT` attendance records generate payout entries
- Confirming attendance on a `BUNDLE` plan decrements `remainingClasses`
- Substituting a teacher on an occurrence does NOT change the schedule's default teacher
- A schedule with `INDIVIDUAL` type enforces `maxStudents = 1`
- Cancelling an occurrence sets all its attendance records to `CANCELLED` status

---

## Database Migration

```sql
-- V5__create_class_management.sql
CREATE TABLE class_schedules (
    id               UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    name             VARCHAR(255) NOT NULL,
    day_of_week      VARCHAR(10) NOT NULL,
    start_time       TIME        NOT NULL,
    duration_minutes INTEGER     NOT NULL,
    teacher_id       UUID        NOT NULL REFERENCES teachers(id),
    type             VARCHAR(20) NOT NULL,
    max_students     INTEGER     NOT NULL DEFAULT 1,
    status           VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at       TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE TABLE class_schedule_students (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    class_schedule_id UUID NOT NULL REFERENCES class_schedules(id),
    student_id        UUID NOT NULL REFERENCES students(id),
    student_plan_id   UUID REFERENCES student_plans(id),
    created_at        TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (class_schedule_id, student_id)
);

CREATE TABLE class_occurrences (
    id                UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    class_schedule_id UUID        NOT NULL REFERENCES class_schedules(id),
    occurrence_date   DATE        NOT NULL,
    teacher_id        UUID        NOT NULL REFERENCES teachers(id),
    status            VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    created_at        TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP   NOT NULL DEFAULT NOW(),
    UNIQUE (class_schedule_id, occurrence_date)
);

CREATE TABLE attendance_records (
    id                   UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    class_occurrence_id  UUID         NOT NULL REFERENCES class_occurrences(id),
    student_id           UUID         NOT NULL REFERENCES students(id),
    status               VARCHAR(20)  NOT NULL DEFAULT 'PRESENT',
    student_billed_value NUMERIC(10,2),
    teacher_payout_value NUMERIC(10,2),
    created_at           TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP    NOT NULL DEFAULT NOW(),
    UNIQUE (class_occurrence_id, student_id)
);

CREATE INDEX idx_occurrences_date       ON class_occurrences (occurrence_date);
CREATE INDEX idx_occurrences_teacher    ON class_occurrences (teacher_id, occurrence_date);
CREATE INDEX idx_attendance_student     ON attendance_records (student_id);
CREATE INDEX idx_attendance_occurrence  ON attendance_records (class_occurrence_id);
```

---

## API Endpoints

| Method | Path                                                         | Role             | Description                          |
|--------|--------------------------------------------------------------|------------------|--------------------------------------|
| GET    | `/api/v1/class-schedules`                                    | OWNER, TEACHER   | List schedules (filterable)          |
| GET    | `/api/v1/class-schedules/{id}`                               | OWNER, TEACHER   | Schedule detail + roster             |
| POST   | `/api/v1/class-schedules`                                    | OWNER            | Create recurring schedule            |
| PUT    | `/api/v1/class-schedules/{id}`                               | OWNER            | Update schedule                      |
| DELETE | `/api/v1/class-schedules/{id}`                               | OWNER            | Deactivate schedule                  |
| POST   | `/api/v1/class-schedules/{id}/students`                      | OWNER            | Add student to schedule              |
| DELETE | `/api/v1/class-schedules/{id}/students/{studentId}`          | OWNER            | Remove student from schedule         |
| GET    | `/api/v1/class-occurrences`                                  | OWNER, TEACHER   | List occurrences (date range filter) |
| GET    | `/api/v1/class-occurrences/{id}`                             | OWNER, TEACHER   | Occurrence detail + attendance       |
| POST   | `/api/v1/class-occurrences/generate`                         | OWNER            | Generate occurrences for date range  |
| PATCH  | `/api/v1/class-occurrences/{id}/teacher`                     | OWNER            | Substitute teacher for occurrence    |
| PATCH  | `/api/v1/class-occurrences/{id}/cancel`                      | OWNER            | Cancel occurrence                    |
| PUT    | `/api/v1/class-occurrences/{id}/attendance`                  | OWNER, TEACHER   | Batch update attendance              |

### Query Params — GET `/api/v1/class-occurrences`

| Param       | Type     | Description                     |
|-------------|----------|---------------------------------|
| `dateFrom`  | `date`   | Start of range (ISO)            |
| `dateTo`    | `date`   | End of range (ISO)              |
| `teacherId` | `UUID`   | Filter by teacher               |
| `studentId` | `UUID`   | Filter by student               |
| `status`    | `String` | SCHEDULED, COMPLETED, CANCELLED |

---

## DTOs

```java
public record CreateClassScheduleRequest(
    @NotBlank String name,
    @NotNull DayOfWeek dayOfWeek,
    @NotNull LocalTime startTime,
    @NotNull @Min(30) Integer durationMinutes,
    @NotNull UUID teacherId,
    @NotNull ClassType type,
    Integer maxStudents
) {}

public record GenerateOccurrencesRequest(
    @NotNull LocalDate fromDate,
    @NotNull LocalDate toDate
) {}

public record UpdateAttendanceRequest(
    @NotNull List<AttendanceEntry> attendances
) {
    public record AttendanceEntry(
        @NotNull UUID studentId,
        @NotNull AttendanceStatus status
    ) {}
}

public record ClassOccurrenceDetailResponse(
    UUID id,
    UUID scheduleId,
    String scheduleName,
    LocalDate occurrenceDate,
    LocalTime startTime,
    Integer durationMinutes,
    UUID teacherId,
    String teacherName,
    OccurrenceStatus status,
    List<AttendanceRecordResponse> attendances
) {}

public record AttendanceRecordResponse(
    UUID studentId,
    String studentName,
    AttendanceStatus status,
    BigDecimal studentBilledValue,
    BigDecimal teacherPayoutValue
) {}

// Weekly calendar view
public record WeeklyScheduleResponse(
    LocalDate weekStart,
    LocalDate weekEnd,
    List<DaySchedule> days
) {
    public record DaySchedule(
        DayOfWeek dayOfWeek,
        LocalDate date,
        List<ClassOccurrenceSummary> classes
    ) {}
}
```

---

## Tests

```java
class ClassManagementControllerTest extends BaseIntegrationTest {

    // --- SCHEDULE ---
    // shouldCreateClassSchedule_withValidData
    // shouldEnforceMaxStudents1_whenTypeIsIndividual
    // shouldAddStudentToSchedule_successfully
    // shouldRejectStudent_whenGroupAtCapacity

    // --- OCCURRENCES ---
    // shouldGenerateOccurrences_forDateRange
    // shouldNotDuplicateOccurrence_forSameScheduleAndDate
    // shouldSubstituteTeacher_withoutChangingScheduleDefault

    // --- ATTENDANCE ---
    // shouldMarkAttendance_andComputePayoutValue
    // shouldDecrementRemainingClasses_whenBundlePlanStudentAttends
    // shouldNotGeneratePayout_whenStudentIsAbsent
    // shouldCancelOccurrence_andMarkAllAttendancesAsCancelled

    // --- FILTERS ---
    // shouldReturnOccurrences_filteredByTeacherAndDateRange
    // shouldReturnOccurrences_filteredByStudent
}
```

---

## Frontend

```
features/classes/
├── classes.routes.ts
├── weekly-calendar/
│   ├── weekly-calendar.component.ts    (main view — grid by day/hour)
│   └── weekly-calendar.component.html
├── class-detail/
│   ├── class-detail.component.ts       (occurrence detail + attendance form)
│   └── class-detail.component.html
├── schedule-form/
│   ├── schedule-form.component.ts
│   └── schedule-form.component.html
├── attendance-form/
│   ├── attendance-form.component.ts    (batch attendance update)
│   └── attendance-form.component.html
└── services/
    └── class.service.ts
```

### Key UI Behaviours

- Weekly calendar: 7-column grid, one column per day, rows per time slot
- Each event card shows: student(s) name, teacher name, status badge
- Today's column is highlighted
- Teacher filter chips at the top (one per teacher, with color coding)
- Clicking an event opens the occurrence detail panel
- Attendance form: list of enrolled students, each with a toggle
  (Present / Absent / Justified). Saving triggers payout computation
- Overdue student warning shown on the attendance card (red badge)
- TEACHER role sees only their own classes
