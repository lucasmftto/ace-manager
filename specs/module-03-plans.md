# Module: Plans & Packages

> Extends `CLAUDE.md`. All base rules apply.

## Overview

Defines the "products" offered by the academy — the types of contracts
available to students. A plan defines **how much** a student pays and
**how often**. Class scheduling (when classes happen) is handled separately
by the Class Management module.

---

## Domain Model

### Entity: `Plan`

```java
@Entity
@Table(name = "plans")
public class Plan extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;                    // e.g. "Individual Monthly"

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlanType type;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal referencePrice;      // base price before any student discount

    // For MONTHLY plans
    private Integer weeklyClassCount;       // how many classes per week
    private Integer billingDayOfMonth;      // day of month for charge generation (1–28)

    // For BUNDLE plans
    private Integer totalClasses;           // total classes in the bundle

    // For GROUP plans
    private Integer maxStudents;            // max capacity

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlanStatus status;
}
```

### Entity: `StudentPlan`

Links a student to a plan (enrollment). The actual billed value is stored
here (may differ from `plan.referencePrice` due to the student's discount).

```java
@Entity
@Table(name = "student_plans")
public class StudentPlan extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id")
    private Teacher teacher;                // primary teacher for this enrollment

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal billedValue;         // actual value charged to this student

    private LocalDate startDate;
    private LocalDate endDate;              // null = indefinite (monthly)

    // For BUNDLE: tracks remaining classes
    private Integer remainingClasses;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StudentPlanStatus status;
}
```

### Enums

```java
public enum PlanType {
    MONTHLY,    // recurring monthly subscription
    BUNDLE,     // pre-paid pack of N classes
    DROP_IN,    // single class, charged per occurrence
    GROUP       // group class with capacity limit
}

public enum PlanStatus {
    ACTIVE, INACTIVE
}

public enum StudentPlanStatus {
    ACTIVE, SUSPENDED, COMPLETED, CANCELLED
}
```

### Business rules

- A student can have **multiple active `StudentPlan`** simultaneously
- `billedValue` must be ≤ plan's `referencePrice` (discounts allowed, surcharges not)
- For `BUNDLE` plans: `remainingClasses` is decremented on each confirmed class attendance
- Alert is triggered when `remainingClasses` ≤ 2
- `billingDayOfMonth` must be between 1 and 28 (avoids end-of-month issues)
- `MONTHLY` plans auto-generate a charge each month (handled by Student Financials module)
- For `GROUP` plans: enrollment is blocked when active enrollments reach `maxStudents`

---

## Database Migration

```sql
-- V4__create_plans.sql
CREATE TABLE plans (
    id                   UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name                 VARCHAR(255) NOT NULL,
    description          TEXT,
    type                 VARCHAR(20)  NOT NULL,
    reference_price      NUMERIC(10,2) NOT NULL,
    weekly_class_count   INTEGER,
    billing_day_of_month INTEGER,
    total_classes        INTEGER,
    max_students         INTEGER,
    status               VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at           TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE student_plans (
    id                UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id        UUID         NOT NULL REFERENCES students(id),
    plan_id           UUID         NOT NULL REFERENCES plans(id),
    teacher_id        UUID         REFERENCES teachers(id),
    billed_value      NUMERIC(10,2) NOT NULL,
    start_date        DATE,
    end_date          DATE,
    remaining_classes INTEGER,
    status            VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at        TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_student_plans_student ON student_plans (student_id) WHERE status = 'ACTIVE';
CREATE INDEX idx_student_plans_plan    ON student_plans (plan_id);
```

---

## API Endpoints

| Method | Path                                   | Role  | Description                       |
|--------|----------------------------------------|-------|-----------------------------------|
| GET    | `/api/v1/plans`                        | OWNER | List all plans                    |
| GET    | `/api/v1/plans/{id}`                   | OWNER | Plan detail                       |
| POST   | `/api/v1/plans`                        | OWNER | Create plan                       |
| PUT    | `/api/v1/plans/{id}`                   | OWNER | Update plan                       |
| DELETE | `/api/v1/plans/{id}`                   | OWNER | Deactivate plan                   |
| GET    | `/api/v1/students/{id}/plans`          | OWNER | Student's active enrollments      |
| POST   | `/api/v1/students/{id}/plans`          | OWNER | Enroll student in a plan          |
| PATCH  | `/api/v1/students/{id}/plans/{planId}` | OWNER | Update enrollment (value, status) |
| DELETE | `/api/v1/students/{id}/plans/{planId}` | OWNER | Cancel enrollment                 |

---

## DTOs

```java
public record CreatePlanRequest(
    @NotBlank String name,
    String description,
    @NotNull PlanType type,
    @NotNull @DecimalMin("0") BigDecimal referencePrice,
    Integer weeklyClassCount,
    @Min(1) @Max(28) Integer billingDayOfMonth,
    @Min(1) Integer totalClasses,
    @Min(1) Integer maxStudents
) {}

public record EnrollStudentRequest(
    @NotNull UUID planId,
    UUID teacherId,
    @NotNull @DecimalMin("0") BigDecimal billedValue,
    @NotNull LocalDate startDate
) {}

public record PlanResponse(
    UUID id,
    String name,
    String description,
    PlanType type,
    BigDecimal referencePrice,
    Integer weeklyClassCount,
    Integer billingDayOfMonth,
    Integer totalClasses,
    Integer maxStudents,
    int activeEnrollments,        // computed
    PlanStatus status
) {}

public record StudentPlanResponse(
    UUID id,
    UUID planId,
    String planName,
    PlanType planType,
    UUID teacherId,
    String teacherName,
    BigDecimal referencePrice,
    BigDecimal billedValue,
    BigDecimal discountAmount,    // computed: referencePrice - billedValue
    LocalDate startDate,
    LocalDate endDate,
    Integer remainingClasses,
    boolean lowClassesAlert,      // true when remainingClasses <= 2
    StudentPlanStatus status
) {}
```

---

## Tests

```java
class PlanControllerTest extends BaseIntegrationTest {

    // shouldCreatePlan_withMonthlyType
    // shouldCreatePlan_withBundleType
    // shouldReturn400_whenBillingDayExceeds28
    // shouldReturn400_whenBundlePlanMissingTotalClasses

    // shouldEnrollStudentInPlan_successfully
    // shouldReturn400_whenBilledValueExceedsReferencePrice
    // shouldBlockEnrollment_whenGroupPlanAtCapacity
    // shouldSetRemainingClasses_whenEnrollingInBundle

    // shouldCancelEnrollment_setsStatusToCancelled
    // shouldReturnActiveEnrollments_forStudent
}
```

---

## Frontend

```
features/plans/
├── plans.routes.ts
├── plans-list/
│   ├── plans-list.component.ts
│   └── plans-list.component.html
├── plan-form/
│   ├── plan-form.component.ts        (type-aware: shows relevant fields per type)
│   └── plan-form.component.html
├── student-enrollment/
│   ├── enrollment-form.component.ts
│   └── enrollment-form.component.html
└── services/
    └── plan.service.ts
```

### Key UI Behaviours

- Plan form dynamically shows/hides fields based on selected `PlanType`:
  - `MONTHLY` → shows `weeklyClassCount` + `billingDayOfMonth`
  - `BUNDLE` → shows `totalClasses`
  - `GROUP` → shows `maxStudents`
- Bundle enrollments show remaining classes with a progress bar
- Low classes alert (≤ 2) shows a warning badge on the enrollment
- Enrollment form pre-fills `billedValue` from `plan.referencePrice` but allows editing downward
