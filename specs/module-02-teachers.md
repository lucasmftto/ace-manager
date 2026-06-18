# Module: Teachers

> Extends `CLAUDE.md`. All base rules apply.

## Overview

Manages teacher registration and payout configuration. Each teacher has a
**payout model** (percentage of student value OR fixed hourly rate). The
model can be overridden per teacher-student pair, allowing individual
agreements without losing central control.

---

## Domain Model

### Entity: `Teacher`

```java
@Entity
@Table(name = "teachers")
public class Teacher extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    private String phone;
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PayoutModel payoutModel;        // PERCENTAGE or HOURLY_RATE

    @Column(precision = 5, scale = 2)
    private BigDecimal defaultPercentage;   // used when model = PERCENTAGE (e.g. 80.00)

    @Column(precision = 10, scale = 2)
    private BigDecimal defaultHourlyRate;   // used when model = HOURLY_RATE (e.g. 100.00)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TeacherStatus status;

    @OneToMany(mappedBy = "teacher", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TeacherStudentConfig> studentConfigs;
}
```

### Entity: `TeacherStudentConfig`

Stores the payout override for a specific teacher-student pair.
When no config exists for a pair, the teacher's default is used.

```java
@Entity
@Table(name = "teacher_student_configs",
       uniqueConstraints = @UniqueConstraint(columnNames = {"teacher_id", "student_id"}))
public class TeacherStudentConfig extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    // Overrides teacher default — null means "use teacher default"
    @Column(precision = 5, scale = 2)
    private BigDecimal overridePercentage;

    @Column(precision = 10, scale = 2)
    private BigDecimal overrideHourlyRate;
}
```

### Enums

```java
public enum PayoutModel {
    PERCENTAGE,    // teacher receives X% of the student's billed value
    HOURLY_RATE    // teacher receives fixed R$/hour
}

public enum TeacherStatus {
    ACTIVE, INACTIVE
}
```

### Business rules

- `defaultPercentage` required when `payoutModel = PERCENTAGE` (0–100)
- `defaultHourlyRate` required when `payoutModel = HOURLY_RATE` (> 0)
- Payout resolution logic (used by Payout module):
  ```
  if TeacherStudentConfig exists for (teacher, student):
      use override value
  else:
      use teacher default
  ```
- A teacher can only have one config per student (unique constraint)
- Deleting a teacher is a soft delete; payout history is preserved

---

## Database Migration

```sql
-- V3__create_teachers.sql
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
    UNIQUE (teacher_id, student_id)
);
```

---

## API Endpoints

| Method | Path                                              | Role  | Description                          |
|--------|---------------------------------------------------|-------|--------------------------------------|
| GET    | `/api/v1/teachers`                                | OWNER | List all teachers                    |
| GET    | `/api/v1/teachers/{id}`                           | OWNER | Teacher detail with student configs  |
| POST   | `/api/v1/teachers`                                | OWNER | Create teacher                       |
| PUT    | `/api/v1/teachers/{id}`                           | OWNER | Update teacher                       |
| DELETE | `/api/v1/teachers/{id}`                           | OWNER | Soft delete                          |
| GET    | `/api/v1/teachers/{id}/student-configs`           | OWNER | List per-student payout configs      |
| PUT    | `/api/v1/teachers/{id}/student-configs/{studentId}` | OWNER | Upsert per-student config          |
| DELETE | `/api/v1/teachers/{id}/student-configs/{studentId}` | OWNER | Remove override (reverts to default) |
| GET    | `/api/v1/teachers/me`                             | TEACHER | Own profile + payout summary       |

---

## DTOs

```java
public record CreateTeacherRequest(
    @NotBlank String name,
    String phone,
    @Email String email,
    @NotNull PayoutModel payoutModel,
    @DecimalMin("0") @DecimalMax("100") BigDecimal defaultPercentage,
    @DecimalMin("0") BigDecimal defaultHourlyRate
) {}

public record UpdateTeacherRequest(
    @NotBlank String name,
    String phone,
    @Email String email,
    @NotNull PayoutModel payoutModel,
    BigDecimal defaultPercentage,
    BigDecimal defaultHourlyRate,
    @NotNull TeacherStatus status
) {}

public record UpsertStudentConfigRequest(
    BigDecimal overridePercentage,
    BigDecimal overrideHourlyRate
) {}

public record TeacherSummaryResponse(
    UUID id,
    String name,
    PayoutModel payoutModel,
    BigDecimal defaultPercentage,
    BigDecimal defaultHourlyRate,
    int activeStudentCount,
    TeacherStatus status
) {}

public record TeacherDetailResponse(
    UUID id,
    String name,
    String phone,
    String email,
    PayoutModel payoutModel,
    BigDecimal defaultPercentage,
    BigDecimal defaultHourlyRate,
    TeacherStatus status,
    List<StudentPayoutConfigResponse> studentConfigs
) {}

public record StudentPayoutConfigResponse(
    UUID studentId,
    String studentName,
    BigDecimal effectivePercentage,      // override if set, otherwise default
    BigDecimal effectiveHourlyRate,
    boolean isOverride                   // true if a specific config exists
) {}
```

---

## Service Methods

```java
public interface TeacherService {
    Page<TeacherSummaryResponse> findAll(Pageable pageable);
    TeacherDetailResponse findById(UUID id);
    TeacherDetailResponse create(CreateTeacherRequest request);
    TeacherDetailResponse update(UUID id, UpdateTeacherRequest request);
    void delete(UUID id);

    List<StudentPayoutConfigResponse> findStudentConfigs(UUID teacherId);
    StudentPayoutConfigResponse upsertStudentConfig(UUID teacherId, UUID studentId, UpsertStudentConfigRequest request);
    void removeStudentConfig(UUID teacherId, UUID studentId);

    // Used by Payout module — resolves effective payout value for a pair
    BigDecimal resolveEffectivePayout(UUID teacherId, UUID studentId, BigDecimal studentValue, int durationMinutes);
}
```

---

## Tests

```java
class TeacherControllerTest extends BaseIntegrationTest {

    // --- CREATE ---
    // shouldCreateTeacher_withPercentageModel
    // shouldCreateTeacher_withHourlyRateModel
    // shouldReturn400_whenPercentageModelMissingDefaultPercentage
    // shouldReturn400_whenHourlyRateModelMissingDefaultRate

    // --- STUDENT CONFIG ---
    // shouldUpsertStudentConfig_createsNewWhenNotExists
    // shouldUpsertStudentConfig_updatesExistingWhenExists
    // shouldRemoveStudentConfig_revertsToDefault
    // shouldReturnEffectiveConfig_usingOverrideWhenExists
    // shouldReturnEffectiveConfig_usingDefaultWhenNoOverride

    // --- PAYOUT RESOLUTION ---
    // shouldResolvePayoutAsPercentage_whenModelIsPercentage
    // shouldResolvePayoutAsHourlyRate_whenModelIsHourlyRate
    // shouldUseOverride_whenStudentConfigExists
    // shouldUseteacherDefault_whenNoStudentConfig
}
```

---

## Frontend

### Files

```
features/teachers/
├── teachers.routes.ts
├── teachers-list/
│   ├── teachers-list.component.ts
│   └── teachers-list.component.html
├── teacher-detail/
│   ├── teacher-detail.component.ts
│   └── teacher-detail.component.html
├── teacher-form/
│   ├── teacher-form.component.ts
│   └── teacher-form.component.html
├── student-config/
│   ├── student-config-table.component.ts   (inline editable table)
│   └── student-config-table.component.html
├── models/
│   └── teacher.model.ts
└── services/
    └── teacher.service.ts
```

### Key UI Behaviours

- Payout model selector toggles between percentage input and hourly rate input
- Student config table is inline-editable: each row shows student name,
  effective value, and an "Override" badge when a specific config is set
- Removing an override shows a confirmation dialog
- Teacher detail shows current month's payout summary (classes count + estimated total)
