# Module: Dashboard

> Extends `CLAUDE.md`. All base rules apply.

## Overview

The first screen users see after login. Read-only aggregation of the most
important daily and monthly data. No new entities — all data is fetched
and computed from the other modules. Each user role sees a different view.

---

## Owner Dashboard Data

### Today's Classes
- All class occurrences for today, sorted by `startTime`
- Each shows: time, student(s), teacher, class type, status badge
- Overdue students flagged with a red badge on the class card

### Monthly Revenue Summary
- `totalExpected`: sum of all PENDING + PAID charges for current month
- `totalReceived`: sum of PAID charges for current month
- `totalOverdue`: sum of OVERDUE charges for current month
- `totalPending`: sum of PENDING charges not yet overdue

### Teacher Payout Summary
- Per teacher: classes count + total payable + closing status

### Alerts
Priority-ordered list of items that need action:
1. Students with overdue charges (count + total amount)
2. Teacher payouts pending closing/payment
3. Upcoming expenses due within 5 days
4. Group classes with available capacity (vacancy alert)

### Group Class Occupancy
- Per group schedule: name, enrolled / max capacity, fill percentage

---

## Teacher Dashboard Data

- Today's own classes only
- Own payout summary for current month (estimated total)
- No financial data from other teachers or students' billing info

---

## Student Dashboard Data

- Own upcoming classes (next 7 days)
- Own active plans with remaining classes (for BUNDLE plans)
- Own pending/overdue charges

---

## API Endpoints

| Method | Path                         | Role    | Description                    |
|--------|------------------------------|---------|--------------------------------|
| GET    | `/api/v1/dashboard`          | ALL     | Returns role-appropriate data  |
| GET    | `/api/v1/dashboard/alerts`   | OWNER   | Actionable alert list          |

The `/api/v1/dashboard` endpoint detects the authenticated user's role
and returns a different response shape accordingly.

---

## DTOs

```java
// Owner response
public record OwnerDashboardResponse(
    LocalDate today,
    List<TodayClassItem> todayClasses,
    RevenueSummary revenueSummary,
    List<TeacherPayoutItem> teacherPayouts,
    List<DashboardAlert> alerts,
    List<GroupOccupancyItem> groupOccupancy
) {
    public record TodayClassItem(
        UUID occurrenceId,
        LocalTime startTime,
        int durationMinutes,
        String className,
        String teacherName,
        List<String> studentNames,
        OccurrenceStatus status,
        boolean hasOverdueStudent
    ) {}

    public record RevenueSummary(
        int month,
        int year,
        BigDecimal totalExpected,
        BigDecimal totalReceived,
        BigDecimal totalOverdue,
        BigDecimal totalPending,
        int overdueStudentCount
    ) {}

    public record TeacherPayoutItem(
        UUID teacherId,
        String teacherName,
        int classesCount,
        BigDecimal totalPayable,
        ClosingStatus closingStatus
    ) {}

    public record DashboardAlert(
        AlertType type,
        AlertSeverity severity,
        String message,
        String actionUrl      // deep link to relevant module
    ) {}

    public record GroupOccupancyItem(
        UUID scheduleId,
        String scheduleName,
        int enrolled,
        int maxStudents,
        double fillPercentage
    ) {}
}

public enum AlertType {
    OVERDUE_STUDENTS,
    PAYOUT_PENDING,
    EXPENSE_DUE_SOON,
    GROUP_VACANCY
}

public enum AlertSeverity {
    HIGH, MEDIUM, LOW
}

// Teacher response
public record TeacherDashboardResponse(
    LocalDate today,
    List<TodayClassItem> todayClasses,
    TeacherMonthSummary monthSummary
) {
    public record TeacherMonthSummary(
        int classesCount,
        BigDecimal estimatedPayout
    ) {}
}

// Student response
public record StudentDashboardResponse(
    List<UpcomingClassItem> upcomingClasses,
    List<ActivePlanItem> activePlans,
    List<PendingChargeItem> pendingCharges
) {}
```

---

## Tests

```java
class DashboardControllerTest extends BaseIntegrationTest {

    // shouldReturnOwnerDashboard_withCorrectRevenueSummary
    // shouldIncludeOverdueStudentFlag_onTodayClasses
    // shouldReturnAlerts_withOverdueStudents
    // shouldReturnAlerts_withPendingPayouts
    // shouldReturnAlerts_withExpensesDueSoon

    // shouldReturnTeacherDashboard_withOnlyOwnClasses
    // shouldNotIncludeFinancialData_inTeacherDashboard

    // shouldReturnStudentDashboard_withOwnClassesAndCharges
    // shouldNotIncludeOtherStudentsData_inStudentDashboard
}
```

---

## Frontend

```
features/dashboard/
├── dashboard.routes.ts
├── owner-dashboard/
│   ├── owner-dashboard.component.ts
│   └── owner-dashboard.component.html
├── teacher-dashboard/
│   ├── teacher-dashboard.component.ts
│   └── teacher-dashboard.component.html
├── student-dashboard/
│   ├── student-dashboard.component.ts
│   └── student-dashboard.component.html
├── widgets/
│   ├── metric-card.component.ts
│   ├── today-classes.component.ts
│   ├── alert-panel.component.ts
│   ├── payout-summary.component.ts
│   └── occupancy-bar.component.ts
└── services/
    └── dashboard.service.ts
```

### Key UI Behaviours

- Route guard checks role and redirects to the correct dashboard component
- Metric cards: 5 cards at the top for owner (revenue metrics + expense warning + net)
- Alert panel: shown only when there are alerts; items sorted by severity (HIGH first)
- Each alert has an action link that navigates to the relevant module
- Today's classes: scrollable list, each card clickable → opens occurrence detail
- Overdue student badge: red background on class card if any enrolled student is overdue
- Group occupancy: progress bar colored by fill (green < 80%, yellow 80–99%, blue = full)
- Auto-refreshes every 5 minutes via a polling interval in the service
