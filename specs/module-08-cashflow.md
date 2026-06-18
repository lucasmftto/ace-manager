# Module: Cash Flow

> Extends `CLAUDE.md`. All base rules apply.

## Overview

Read-only consolidation module. Aggregates data from Student Financials,
Teacher Payouts and Operational Expenses to show the monthly P&L.
Replaces the "Fluxo de caixa" tab of the existing spreadsheet — but now
computed automatically instead of filled manually.

No new entities — this module only **reads and aggregates** existing data.

---

## Business logic

```
Monthly Cash Flow:

  Revenue
    + Student payments received (StudentCharge.status = PAID, in reference month)
    + Other income (manually added)

  Outflows — Teacher Payouts
    - Sum of TeacherPayoutClosing.totalPayable where status = PAID, in reference month
    - Pending payouts listed separately (not yet deducted)

  Outflows — Operational Expenses
    - Sum of Expense.amount where status = PAID, in reference month

  Net Result
    = Revenue − Paid Teacher Payouts − Paid Operational Expenses
```

---

## API Endpoints

| Method | Path                                  | Role  | Description                            |
|--------|---------------------------------------|-------|----------------------------------------|
| GET    | `/api/v1/cash-flow/monthly`           | OWNER | Full monthly P&L for a given month     |
| GET    | `/api/v1/cash-flow/history`           | OWNER | Month-by-month summary (last 12 months)|

### Query Params — GET `/api/v1/cash-flow/monthly`

| Param   | Type  | Description |
|---------|-------|-------------|
| `month` | `int` | 1–12        |
| `year`  | `int` | e.g. 2026   |

---

## DTOs

```java
public record MonthlyCashFlowResponse(
    Integer month,
    Integer year,

    // Revenue
    BigDecimal studentRevenue,
    BigDecimal otherRevenue,
    BigDecimal totalRevenue,

    // Teacher payouts
    BigDecimal paidTeacherPayouts,
    BigDecimal pendingTeacherPayouts,     // informational only
    List<TeacherPayoutLineItem> teacherPayoutBreakdown,

    // Operational expenses
    BigDecimal paidExpenses,
    BigDecimal pendingExpenses,           // informational only
    List<ExpenseLineItem> expenseBreakdown,

    // Result
    BigDecimal netResult,                 // totalRevenue - paidPayouts - paidExpenses
    String netResultSign                  // "positive" or "negative"
) {
    public record TeacherPayoutLineItem(
        String teacherName,
        BigDecimal amount,
        ClosingStatus status
    ) {}

    public record ExpenseLineItem(
        String name,
        ExpenseCategory category,
        BigDecimal amount,
        ExpenseStatus status
    ) {}
}

public record CashFlowHistoryResponse(
    List<MonthSummary> months
) {
    public record MonthSummary(
        Integer month,
        Integer year,
        BigDecimal totalRevenue,
        BigDecimal totalOutflows,
        BigDecimal netResult
    ) {}
}
```

---

## Tests

```java
class CashFlowControllerTest extends BaseIntegrationTest {

    // shouldComputeMonthlyFlow_withRevenueAndExpenses
    // shouldComputeNegativeNetResult_whenExpensesExceedRevenue
    // shouldIncludePendingPayouts_asInformationalOnly
    // shouldReturnHistory_forLast12Months
    // shouldReturn403_whenTeacherTriesToAccessCashFlow
}
```

---

## Frontend

```
features/cashflow/
├── cashflow.routes.ts
├── cashflow/
│   ├── cashflow.component.ts
│   └── cashflow.component.html
└── services/
    └── cashflow.service.ts
```

### Key UI Behaviours

- Two-column layout: left = "Dinheiro Entrando" (green), right = "Dinheiro Saindo" (red)
- Each side is a card with a list of line items and a subtotal
- Bottom bar: dark background, shows net result in large text (green if positive)
- Month selector at top right
- Pending items shown with a muted style and "(pendente)" label — not counted in net
- Line items link to the originating module (click expense → goes to Expenses)
