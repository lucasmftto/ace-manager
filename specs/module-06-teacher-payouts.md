# Module: Teacher Payouts

> Extends `CLAUDE.md`. All base rules apply.

## Overview

Calculates and tracks what each teacher is owed each month.
The total payout = **base commission** (from confirmed class attendance) +
**extra credit entries** (drop-in classes, materials, transport) −
**extra debit entries** (advances paid during the month).

---

## Domain Model

### Entity: `TeacherPayoutEntry`

Auto-generated from confirmed attendance records (one entry per attendance).

```java
@Entity
@Table(name = "teacher_payout_entries")
public class TeacherPayoutEntry extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attendance_record_id")
    private AttendanceRecord attendanceRecord;  // null for manual entries

    @Column(nullable = false)
    private Integer referenceMonth;

    @Column(nullable = false)
    private Integer referenceYear;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PayoutEntryType type;   // COMMISSION, EXTRA_CREDIT, EXTRA_DEBIT

    @Column(nullable = false)
    private String description;     // e.g. "Class - Abel - 03/06", "Ball purchase", "Advance"

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal amount;      // always positive; sign comes from type

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private Student student;        // null for non-commission entries
}
```

### Entity: `TeacherPayoutClosing`

Monthly closing record — marks the month as done and tracks payment.

```java
@Entity
@Table(name = "teacher_payout_closings",
       uniqueConstraints = @UniqueConstraint(columnNames = {"teacher_id", "reference_month", "reference_year"}))
public class TeacherPayoutClosing extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;

    @Column(nullable = false)
    private Integer referenceMonth;

    @Column(nullable = false)
    private Integer referenceYear;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal totalCommission;     // sum of COMMISSION entries

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal totalExtraCredits;   // sum of EXTRA_CREDIT entries

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal totalExtraDebits;    // sum of EXTRA_DEBIT entries

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal totalPayable;        // commission + credits - debits

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClosingStatus status;

    private LocalDate paidDate;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
```

### Enums

```java
public enum PayoutEntryType {
    COMMISSION,     // auto-generated from attendance
    EXTRA_CREDIT,   // manual: drop-in extra, ball, transport, etc.
    EXTRA_DEBIT     // manual: advance deducted from payout
}

public enum ClosingStatus {
    OPEN,    // month still in progress
    CLOSED,  // calculated but not yet paid
    PAID     // transfer confirmed
}
```

### Business rules

- `COMMISSION` entries are **auto-created** when attendance is confirmed as `PRESENT`
- If attendance is changed from `PRESENT` to `ABSENT`, the payout entry is deleted
- `EXTRA_CREDIT` and `EXTRA_DEBIT` are added manually by the owner
- `totalPayable = totalCommission + totalExtraCredits − totalExtraDebits`
- A closing cannot be created for a month that has no entries
- Once `status = PAID`, no new entries can be added for that month
- Payout value per attendance is resolved at confirmation time via
  `TeacherService.resolveEffectivePayout(teacherId, studentId, billedValue, durationMinutes)`
  and stored in `AttendanceRecord.teacherPayoutValue`

---

## Database Migration

```sql
-- V7__create_teacher_payouts.sql
CREATE TABLE teacher_payout_entries (
    id                   UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    teacher_id           UUID         NOT NULL REFERENCES teachers(id),
    attendance_record_id UUID         REFERENCES attendance_records(id),
    reference_month      INTEGER      NOT NULL,
    reference_year       INTEGER      NOT NULL,
    type                 VARCHAR(20)  NOT NULL,
    description          VARCHAR(255) NOT NULL,
    amount               NUMERIC(10,2) NOT NULL,
    student_id           UUID         REFERENCES students(id),
    created_at           TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE teacher_payout_closings (
    id                 UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    teacher_id         UUID         NOT NULL REFERENCES teachers(id),
    reference_month    INTEGER      NOT NULL,
    reference_year     INTEGER      NOT NULL,
    total_commission   NUMERIC(10,2) NOT NULL,
    total_extra_credits NUMERIC(10,2) NOT NULL DEFAULT 0,
    total_extra_debits  NUMERIC(10,2) NOT NULL DEFAULT 0,
    total_payable      NUMERIC(10,2) NOT NULL,
    status             VARCHAR(20)  NOT NULL DEFAULT 'OPEN',
    paid_date          DATE,
    notes              TEXT,
    created_at         TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMP    NOT NULL DEFAULT NOW(),
    UNIQUE (teacher_id, reference_month, reference_year)
);

CREATE INDEX idx_payout_entries_teacher ON teacher_payout_entries (teacher_id, reference_year, reference_month);
CREATE INDEX idx_payout_closings_teacher ON teacher_payout_closings (teacher_id);
```

---

## API Endpoints

| Method | Path                                                            | Role             | Description                           |
|--------|-----------------------------------------------------------------|------------------|---------------------------------------|
| GET    | `/api/v1/teacher-payouts`                                       | OWNER            | List payout summaries by month        |
| GET    | `/api/v1/teacher-payouts/{teacherId}/summary`                   | OWNER, TEACHER   | Teacher payout summary for a month    |
| GET    | `/api/v1/teacher-payouts/{teacherId}/entries`                   | OWNER, TEACHER   | Detailed entries for a month          |
| POST   | `/api/v1/teacher-payouts/{teacherId}/entries`                   | OWNER            | Add manual extra entry (credit/debit) |
| DELETE | `/api/v1/teacher-payouts/{teacherId}/entries/{entryId}`         | OWNER            | Remove manual entry                   |
| POST   | `/api/v1/teacher-payouts/{teacherId}/close`                     | OWNER            | Create monthly closing                |
| PATCH  | `/api/v1/teacher-payouts/{teacherId}/closings/{closingId}/pay`  | OWNER            | Mark closing as paid                  |
| GET    | `/api/v1/teacher-payouts/{teacherId}/closings`                  | OWNER, TEACHER   | Closing history                       |

---

## DTOs

```java
public record AddPayoutEntryRequest(
    @NotNull Integer referenceMonth,
    @NotNull Integer referenceYear,
    @NotNull PayoutEntryType type,          // EXTRA_CREDIT or EXTRA_DEBIT only (not COMMISSION)
    @NotBlank String description,
    @NotNull @DecimalMin("0.01") BigDecimal amount,
    UUID studentId                          // optional reference
) {}

public record TeacherPayoutSummaryResponse(
    UUID teacherId,
    String teacherName,
    Integer referenceMonth,
    Integer referenceYear,
    int totalClassesCount,
    BigDecimal totalCommission,
    BigDecimal totalExtraCredits,
    BigDecimal totalExtraDebits,
    BigDecimal totalPayable,
    ClosingStatus closingStatus,            // null if no closing yet
    LocalDate paidDate
) {}

public record PayoutEntryResponse(
    UUID id,
    PayoutEntryType type,
    String description,
    BigDecimal amount,
    String sign,                            // "+" for COMMISSION/EXTRA_CREDIT, "-" for EXTRA_DEBIT
    UUID studentId,
    String studentName,
    LocalDate classDate,                    // from attendance record, if applicable
    boolean isManual                        // false = auto from attendance
) {}

public record PayoutClosingResponse(
    UUID id,
    Integer referenceMonth,
    Integer referenceYear,
    BigDecimal totalCommission,
    BigDecimal totalExtraCredits,
    BigDecimal totalExtraDebits,
    BigDecimal totalPayable,
    ClosingStatus status,
    LocalDate paidDate,
    String notes
) {}

// For the payouts overview page
public record MonthlyPayoutsOverviewResponse(
    Integer referenceMonth,
    Integer referenceYear,
    BigDecimal totalPayable,
    BigDecimal totalPaid,
    BigDecimal totalPending,
    List<TeacherPayoutSummaryResponse> teachers
) {}
```

---

## Tests

```java
class TeacherPayoutControllerTest extends BaseIntegrationTest {

    // --- COMMISSION AUTO-GENERATION ---
    // shouldCreatePayoutEntry_whenAttendanceConfirmedAsPresent
    // shouldDeletePayoutEntry_whenAttendanceChangedToAbsent
    // shouldUseStudentSpecificConfig_whenComputingCommission
    // shouldUseTeacherDefault_whenNoStudentConfig

    // --- MANUAL ENTRIES ---
    // shouldAddExtraCreditEntry_successfully
    // shouldAddExtraDebitEntry_successfully
    // shouldReturn400_whenTryingToAddCommissionManually
    // shouldDeleteManualEntry_successfully
    // shouldReturn400_whenDeletingAutoCommissionEntry

    // --- CLOSING ---
    // shouldCreateClosing_withCorrectTotals
    // shouldReturn400_whenClosingHasNoEntries
    // shouldReturn400_whenClosingAlreadyExists
    // shouldMarkClosingAsPaid_successfully
    // shouldReturn400_whenAddingEntryToAlreadyPaidMonth

    // --- TEACHER ROLE ---
    // shouldReturnOwnSummary_whenTeacherCallsMeEndpoint
    // shouldReturn403_whenTeacherViewsOtherTeacherPayout
}
```

---

## Frontend

```
features/payouts/
├── payouts.routes.ts
├── payouts-overview/
│   ├── payouts-overview.component.ts     (3-column card layout per teacher)
│   └── payouts-overview.component.html
├── payout-detail/
│   ├── payout-detail.component.ts        (entries table + add manual entry)
│   └── payout-detail.component.html
├── add-entry-dialog/
│   ├── add-entry-dialog.component.ts
│   └── add-entry-dialog.component.html
└── services/
    └── payout.service.ts
```

### Key UI Behaviours

- Overview shows 3 summary cards (one per teacher): name, class count,
  commission, extras summary, total, and status badge
- Each card has a breakdown section showing per-student amounts
- Entry table groups by type: Commissions section → Extra Credits section → Extra Debits section
- Credit amounts shown in green with "+" prefix; debit in red with "−" prefix
- Auto-generated commission entries show the class date and student name
- Manual entries show a delete icon; auto entries do not
- "Close month" button calculates and locks the month
- "Mark as paid" button on closed months; shows paid date once confirmed
- TEACHER role sees only their own payout summary (read-only, no edit/close actions)
