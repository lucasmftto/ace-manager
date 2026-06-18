# Module: Student Financials

> Extends `CLAUDE.md`. All base rules apply.

## Overview

Controls billing and payment tracking for students. Monthly charges are
**auto-generated** for MONTHLY plans on their configured billing day.
Tracks payment status and drives the overdue alerts seen in the Dashboard.

---

## Domain Model

### Entity: `StudentCharge`

```java
@Entity
@Table(name = "student_charges")
public class StudentCharge extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_plan_id")
    private StudentPlan studentPlan;

    @Column(nullable = false)
    private String description;           // e.g. "Monthly fee — June 2026"

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDate dueDate;

    private LocalDate paidDate;

    @Column(precision = 10, scale = 2)
    private BigDecimal paidAmount;        // may differ (partial payment)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChargeStatus status;

    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;  // how it was paid

    @Column(columnDefinition = "TEXT")
    private String notes;

    // Reference month for monthly charges
    private Integer referenceMonth;       // 1–12
    private Integer referenceYear;
}
```

### Enums

```java
public enum ChargeStatus {
    PENDING,    // due date not yet reached
    OVERDUE,    // due date passed, not paid
    PAID,       // fully paid
    CANCELLED   // charge voided
}
```

### Business rules

- Charges are auto-generated on the `billingDayOfMonth` of each month
  for all active MONTHLY `StudentPlan` records
- One charge per student per plan per month (no duplicates)
- Status transitions:
  - `PENDING` → `PAID` (manual confirmation by owner)
  - `PENDING` → `OVERDUE` (auto, triggered by a scheduled job when `dueDate < today`)
  - `OVERDUE` → `PAID` (manual confirmation)
  - `PENDING` or `OVERDUE` → `CANCELLED` (manual)
- `paidAmount` defaults to `amount` when confirming payment but can be adjusted
- The **financial status** shown on the student list is derived from:
  the most recent active charge that is `PENDING` or `OVERDUE`

---

## Database Migration

```sql
-- V6__create_student_charges.sql
CREATE TABLE student_charges (
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id       UUID         NOT NULL REFERENCES students(id),
    student_plan_id  UUID         REFERENCES student_plans(id),
    description      VARCHAR(255) NOT NULL,
    amount           NUMERIC(10,2) NOT NULL,
    due_date         DATE         NOT NULL,
    paid_date        DATE,
    paid_amount      NUMERIC(10,2),
    status           VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    payment_method   VARCHAR(20),
    notes            TEXT,
    reference_month  INTEGER,
    reference_year   INTEGER,
    created_at       TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP    NOT NULL DEFAULT NOW(),
    UNIQUE (student_plan_id, reference_month, reference_year)
);

CREATE INDEX idx_charges_student     ON student_charges (student_id);
CREATE INDEX idx_charges_due_date    ON student_charges (due_date) WHERE status IN ('PENDING', 'OVERDUE');
CREATE INDEX idx_charges_status      ON student_charges (status);
```

---

## API Endpoints

| Method | Path                                            | Role    | Description                        |
|--------|-------------------------------------------------|---------|------------------------------------|
| GET    | `/api/v1/student-charges`                       | OWNER   | List charges (filterable)          |
| GET    | `/api/v1/student-charges/{id}`                  | OWNER   | Charge detail                      |
| POST   | `/api/v1/student-charges`                       | OWNER   | Create charge manually             |
| POST   | `/api/v1/student-charges/generate-monthly`      | OWNER   | Trigger monthly charge generation  |
| PATCH  | `/api/v1/student-charges/{id}/pay`              | OWNER   | Mark charge as paid                |
| PATCH  | `/api/v1/student-charges/{id}/cancel`           | OWNER   | Cancel charge                      |
| GET    | `/api/v1/students/{id}/charges`                 | OWNER   | Student's charge history           |
| GET    | `/api/v1/student-charges/summary`               | OWNER   | Monthly summary (totals by status) |
| GET    | `/api/v1/student-charges/me`                    | STUDENT | Own charge history                 |

### Query Params — GET `/api/v1/student-charges`

| Param            | Type           | Description                  |
|------------------|----------------|------------------------------|
| `status`         | `ChargeStatus` | Filter by status             |
| `studentId`      | `UUID`         | Filter by student            |
| `referenceMonth` | `int`          | Month (1–12)                 |
| `referenceYear`  | `int`          | Year                         |
| `dueDateFrom`    | `date`         | Due date range start         |
| `dueDateTo`      | `date`         | Due date range end           |

---

## DTOs

```java
public record CreateChargeRequest(
    @NotNull UUID studentId,
    UUID studentPlanId,
    @NotBlank String description,
    @NotNull @DecimalMin("0") BigDecimal amount,
    @NotNull LocalDate dueDate,
    Integer referenceMonth,
    Integer referenceYear,
    String notes
) {}

public record ConfirmPaymentRequest(
    @NotNull LocalDate paidDate,
    @NotNull @DecimalMin("0") BigDecimal paidAmount,
    @NotNull PaymentMethod paymentMethod,
    String notes
) {}

public record GenerateMonthlyChargesRequest(
    @NotNull Integer month,
    @NotNull Integer year
) {}

public record StudentChargeResponse(
    UUID id,
    UUID studentId,
    String studentName,
    PaymentMethod preferredPaymentMethod,   // from student profile
    String description,
    BigDecimal amount,
    LocalDate dueDate,
    LocalDate paidDate,
    BigDecimal paidAmount,
    ChargeStatus status,
    PaymentMethod paymentMethod,
    Integer referenceMonth,
    Integer referenceYear,
    String notes,
    int daysOverdue                          // computed: today - dueDate if OVERDUE
) {}

public record FinancialSummaryResponse(
    Integer month,
    Integer year,
    BigDecimal totalExpected,
    BigDecimal totalReceived,
    BigDecimal totalPending,
    BigDecimal totalOverdue,
    int overdueStudentCount,
    int paidCount,
    int pendingCount,
    int overdueCount
) {}
```

---

## Scheduled Job

```java
// Runs daily at 01:00 to mark overdue charges
@Component
public class ChargeOverdueJob {

    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    public void markOverdueCharges() {
        // Update all PENDING charges where dueDate < today → OVERDUE
    }
}
```

---

## Tests

```java
class StudentFinancialControllerTest extends BaseIntegrationTest {

    // --- MANUAL CHARGE ---
    // shouldCreateCharge_manually
    // shouldReturn400_whenAmountIsZero

    // --- AUTO GENERATION ---
    // shouldGenerateMonthlyCharges_forAllActiveMonthlyPlans
    // shouldNotDuplicateCharge_whenGeneratedTwiceForSameMonth
    // shouldSkipInactivePlans_duringGeneration

    // --- PAYMENT ---
    // shouldMarkChargeAsPaid_withCorrectDate
    // shouldAllowPartialPayment_lessThanAmount
    // shouldReturn400_whenConfirmingAlreadyPaidCharge

    // --- OVERDUE ---
    // shouldMarkChargeAsOverdue_whenDueDatePassed  (manually call the job)
    // shouldReturnOverdueCharges_inFilteredList

    // --- SUMMARY ---
    // shouldReturnCorrectSummary_forGivenMonthAndYear

    // --- STUDENT ROLE ---
    // shouldReturnOnlyOwnCharges_whenStudentCallsMeEndpoint
}
```

---

## Frontend

```
features/financials/
├── financials.routes.ts
├── charges-list/
│   ├── charges-list.component.ts     (main table with filters + tabs)
│   └── charges-list.component.html
├── charge-detail/
│   ├── charge-detail.component.ts
│   └── charge-detail.component.html
├── payment-dialog/
│   ├── payment-dialog.component.ts   (confirm payment modal)
│   └── payment-dialog.component.html
└── services/
    └── financial.service.ts
```

### Key UI Behaviours

- Top 4 metric cards: expected / received / pending / overdue
- Tab bar: All | Overdue | Pending | Paid
- Each row shows: student name, preferred payment method badge, amount,
  due date, status badge, and a contextual action button
  - PENDING: "Confirm payment" button
  - OVERDUE: "Charge" button (highlighted red)
  - PAID: "View details" button
- Due date column turns red if charge is overdue
- "Generate monthly charges" button with a month/year picker
- Payment confirmation dialog shows pre-filled amount (editable)
  and payment method selector
