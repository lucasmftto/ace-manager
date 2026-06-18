# Module: Operational Expenses

> Extends `CLAUDE.md`. All base rules apply.

## Overview

Tracks all operational costs of the academy beyond teacher payouts — rent,
taxes, accounting, utilities, etc. Identified from the "Fluxo de caixa" tab
of the existing spreadsheet. Fixed expenses recur monthly; variable expenses
are entered manually.

---

## Domain Model

### Entity: `Expense`

```java
@Entity
@Table(name = "expenses")
public class Expense extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;                   // e.g. "Imposto", "Play tênis"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExpenseCategory category;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDate dueDate;

    private LocalDate paidDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExpenseStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExpenseRecurrence recurrence;  // MONTHLY or ONE_TIME

    // For MONTHLY expenses: day of month for auto-generation (1–28)
    private Integer billingDayOfMonth;

    private Integer referenceMonth;
    private Integer referenceYear;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
```

### Enums

```java
public enum ExpenseCategory {
    FINANCIAL,      // cartão, banco
    BENEFITS,       // convênio
    TAX,            // imposto
    ACCOUNTING,     // contabilidade
    RENT,           // aluguel (play tênis, EFG)
    TRANSPORT,
    MARKETING,      // insta
    MATERIAL,       // bolas, equipamentos
    OTHER
}

public enum ExpenseStatus {
    PENDING, PAID, CANCELLED
}

public enum ExpenseRecurrence {
    MONTHLY,    // auto-generated each month
    ONE_TIME    // single occurrence
}
```

### Business rules

- `MONTHLY` expenses are auto-generated on their `billingDayOfMonth` each month
- One expense per name/category per month (no duplicates for recurring ones)
- Status transitions: `PENDING` → `PAID` or `PENDING` → `CANCELLED`
- Auto-generation uses the same pattern as Student Financials monthly charges

---

## Database Migration

```sql
-- V8__create_expenses.sql
CREATE TABLE expenses (
    id                   UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name                 VARCHAR(255) NOT NULL,
    category             VARCHAR(20)  NOT NULL,
    amount               NUMERIC(10,2) NOT NULL,
    due_date             DATE         NOT NULL,
    paid_date            DATE,
    status               VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    recurrence           VARCHAR(20)  NOT NULL DEFAULT 'ONE_TIME',
    billing_day_of_month INTEGER,
    reference_month      INTEGER,
    reference_year       INTEGER,
    notes                TEXT,
    created_at           TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_expenses_status   ON expenses (status);
CREATE INDEX idx_expenses_due_date ON expenses (due_date) WHERE status = 'PENDING';
CREATE INDEX idx_expenses_ref      ON expenses (reference_year, reference_month);
```

---

## API Endpoints

| Method | Path                                         | Role  | Description                         |
|--------|----------------------------------------------|-------|-------------------------------------|
| GET    | `/api/v1/expenses`                           | OWNER | List expenses (filterable)          |
| GET    | `/api/v1/expenses/{id}`                      | OWNER | Expense detail                      |
| POST   | `/api/v1/expenses`                           | OWNER | Create expense (manual or template) |
| PUT    | `/api/v1/expenses/{id}`                      | OWNER | Update expense                      |
| PATCH  | `/api/v1/expenses/{id}/pay`                  | OWNER | Mark as paid                        |
| PATCH  | `/api/v1/expenses/{id}/cancel`               | OWNER | Cancel expense                      |
| DELETE | `/api/v1/expenses/{id}`                      | OWNER | Hard delete (only PENDING/CANCELLED)|
| POST   | `/api/v1/expenses/generate-monthly`          | OWNER | Trigger monthly generation          |
| GET    | `/api/v1/expenses/summary`                   | OWNER | Monthly totals by status/category   |

---

## DTOs

```java
public record CreateExpenseRequest(
    @NotBlank String name,
    @NotNull ExpenseCategory category,
    @NotNull @DecimalMin("0") BigDecimal amount,
    @NotNull LocalDate dueDate,
    @NotNull ExpenseRecurrence recurrence,
    @Min(1) @Max(28) Integer billingDayOfMonth,
    Integer referenceMonth,
    Integer referenceYear,
    String notes
) {}

public record ConfirmExpensePaymentRequest(
    @NotNull LocalDate paidDate,
    String notes
) {}

public record ExpenseResponse(
    UUID id,
    String name,
    ExpenseCategory category,
    BigDecimal amount,
    LocalDate dueDate,
    LocalDate paidDate,
    ExpenseStatus status,
    ExpenseRecurrence recurrence,
    Integer referenceMonth,
    Integer referenceYear,
    String notes,
    int daysUntilDue   // negative if overdue
) {}

public record ExpenseSummaryResponse(
    Integer month,
    Integer year,
    BigDecimal totalExpected,
    BigDecimal totalPaid,
    BigDecimal totalPending,
    Map<ExpenseCategory, BigDecimal> byCategory
) {}
```

---

## Tests

```java
class ExpenseControllerTest extends BaseIntegrationTest {

    // shouldCreateOneTimeExpense_successfully
    // shouldCreateMonthlyExpense_withBillingDay
    // shouldGenerateMonthlyExpenses_forAllRecurringTemplates
    // shouldNotDuplicateExpense_whenGeneratedTwiceForSameMonth
    // shouldMarkExpenseAsPaid_successfully
    // shouldCancelExpense_successfully
    // shouldReturn400_whenDeletingPaidExpense
    // shouldReturnCorrectSummary_groupedByCategory
    // shouldReturn403_whenTeacherTriesToAccessExpenses
}
```

---

## Frontend

```
features/expenses/
├── expenses.routes.ts
├── expenses-list/
│   ├── expenses-list.component.ts
│   └── expenses-list.component.html
├── expense-form/
│   ├── expense-form.component.ts
│   └── expense-form.component.html
└── services/
    └── expense.service.ts
```

### Key UI Behaviours

- Summary cards at top: total expected / paid / pending
- Table with columns: name, category chip, amount, due date, status, action
- Due date is highlighted red if the expense is pending and due within 3 days
- "Mark as paid" inline button on each PENDING row
- Recurrence badge on MONTHLY expenses
- "Generate monthly expenses" button with month/year picker
