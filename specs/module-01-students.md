# Module: Students

> Extends `CLAUDE.md`. All base rules apply.

## Overview

Central registry of all academy students. Each student has a **agreed value**
(original contract price) and a **current value** (what they actually pay,
which may be lower due to a permanent discount). Payment method and free-form
notes are also stored per student.

---

## Domain Model

### Entity: `Student`

```java
@Entity
@Table(name = "students")
public class Student extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    private String phone;
    private String email;
    private LocalDate birthDate;

    // Guardian required if student is a minor (under 18)
    private String guardianName;
    private String guardianPhone;

    // Billing values
    @Column(precision = 10, scale = 2)
    private BigDecimal agreedMonthlyValue;   // original contract value

    @Column(precision = 10, scale = 2)
    private BigDecimal currentMonthlyValue;  // actual billed value (may differ)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod preferredPaymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StudentStatus status;

    @Column(columnDefinition = "TEXT")
    private String notes;                    // free-form observations
}
```

### Enums

```java
public enum PaymentMethod {
    PIX, BOLETO, PIX_AUTOMATIC, OTHER
}

public enum StudentStatus {
    ACTIVE, INACTIVE, SUSPENDED
}
```

### Business rules

- `currentMonthlyValue` cannot exceed `agreedMonthlyValue`
- If `birthDate` indicates the student is under 18, `guardianName` and
  `guardianPhone` are required
- Deleting a student is a **soft delete** — sets `deleted_at`; active
  charges and class history are preserved
- A student can have **multiple active plans** simultaneously

---

## Database Migration

```sql
-- V2__create_students.sql
CREATE TABLE students (
    id                        UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name                      VARCHAR(255) NOT NULL,
    phone                     VARCHAR(20),
    email                     VARCHAR(255),
    birth_date                DATE,
    guardian_name             VARCHAR(255),
    guardian_phone            VARCHAR(20),
    agreed_monthly_value      NUMERIC(10,2),
    current_monthly_value     NUMERIC(10,2),
    preferred_payment_method  VARCHAR(20)  NOT NULL DEFAULT 'PIX',
    status                    VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    notes                     TEXT,
    created_at                TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at                TIMESTAMP    NOT NULL DEFAULT NOW(),
    deleted_at                TIMESTAMP
);

CREATE INDEX idx_students_status ON students (status) WHERE deleted_at IS NULL;
CREATE INDEX idx_students_name   ON students (name)   WHERE deleted_at IS NULL;
```

---

## API Endpoints

| Method | Path                     | Role         | Description                       |
|--------|--------------------------|--------------|-----------------------------------|
| GET    | `/api/v1/students`       | OWNER        | List students (paginated, filters)|
| GET    | `/api/v1/students/{id}`  | OWNER        | Get student detail                |
| POST   | `/api/v1/students`       | OWNER        | Create student                    |
| PUT    | `/api/v1/students/{id}`  | OWNER        | Update student                    |
| DELETE | `/api/v1/students/{id}`  | OWNER        | Soft delete student               |
| GET    | `/api/v1/students/{id}/classes`   | OWNER | Student class history        |
| GET    | `/api/v1/students/{id}/charges`   | OWNER | Student charge history       |
| GET    | `/api/v1/students/me`    | STUDENT      | Logged-in student own profile     |

### Query Params — GET `/api/v1/students`

| Param    | Type              | Description                   |
|----------|-------------------|-------------------------------|
| `status` | `StudentStatus`   | Filter by status              |
| `search` | `String`          | Name or email contains        |
| `page`   | `int` (default 0) | Page number                   |
| `size`   | `int` (default 20)| Page size                     |
| `sort`   | `String`          | e.g. `name,asc`               |

---

## DTOs

```java
// POST /students
public record CreateStudentRequest(
    @NotBlank String name,
    String phone,
    @Email String email,
    LocalDate birthDate,
    String guardianName,
    String guardianPhone,
    @NotNull @DecimalMin("0.0") BigDecimal agreedMonthlyValue,
    @NotNull @DecimalMin("0.0") BigDecimal currentMonthlyValue,
    @NotNull PaymentMethod preferredPaymentMethod,
    String notes
) {}

// PUT /students/{id}
public record UpdateStudentRequest(
    @NotBlank String name,
    String phone,
    @Email String email,
    LocalDate birthDate,
    String guardianName,
    String guardianPhone,
    @NotNull @DecimalMin("0.0") BigDecimal agreedMonthlyValue,
    @NotNull @DecimalMin("0.0") BigDecimal currentMonthlyValue,
    @NotNull PaymentMethod preferredPaymentMethod,
    @NotNull StudentStatus status,
    String notes
) {}

// GET /students (list item)
public record StudentSummaryResponse(
    UUID id,
    String name,
    String phone,
    BigDecimal currentMonthlyValue,
    PaymentMethod preferredPaymentMethod,
    StudentStatus status,
    ChargeStatus financialStatus   // computed from latest charge
) {}

// GET /students/{id} (detail)
public record StudentDetailResponse(
    UUID id,
    String name,
    String phone,
    String email,
    LocalDate birthDate,
    String guardianName,
    String guardianPhone,
    BigDecimal agreedMonthlyValue,
    BigDecimal currentMonthlyValue,
    BigDecimal discountAmount,        // computed: agreed - current
    BigDecimal discountPercentage,    // computed: (1 - current/agreed) * 100
    PaymentMethod preferredPaymentMethod,
    StudentStatus status,
    String notes,
    LocalDateTime createdAt
) {}
```

---

## Service Methods

```java
public interface StudentService {
    Page<StudentSummaryResponse> findAll(StudentFilterParams filters, Pageable pageable);
    StudentDetailResponse findById(UUID id);
    StudentDetailResponse create(CreateStudentRequest request);
    StudentDetailResponse update(UUID id, UpdateStudentRequest request);
    void delete(UUID id);
    StudentDetailResponse findCurrentStudentProfile(); // for STUDENT role
}
```

---

## Tests

```java
class StudentControllerTest extends BaseIntegrationTest {

    // --- CREATE ---
    // shouldCreateStudent_whenValidRequest
    // shouldReturn400_whenNameIsBlank
    // shouldReturn400_whenCurrentValueExceedsAgreedValue
    // shouldRequireGuardian_whenStudentIsMinor

    // --- READ ---
    // shouldReturnStudentDetail_whenFound
    // shouldReturn404_whenStudentNotFound
    // shouldListStudents_withPagination
    // shouldFilterStudents_byStatus
    // shouldFilterStudents_byNameSearch

    // --- UPDATE ---
    // shouldUpdateStudent_whenValidRequest
    // shouldReturn404_whenUpdatingNonExistentStudent

    // --- DELETE ---
    // shouldSoftDeleteStudent_whenExists
    // shouldNotReturnDeletedStudents_inListing

    // --- ROLE ---
    // shouldReturn403_whenTeacherTriesToListAllStudents
    // shouldReturnOwnProfile_whenStudentCallsMeEndpoint
}
```

---

## Frontend

### Files

```
features/students/
├── students.routes.ts
├── students-list/
│   ├── students-list.component.ts
│   └── students-list.component.html
├── student-detail/
│   ├── student-detail.component.ts
│   └── student-detail.component.html
├── student-form/
│   ├── student-form.component.ts     (used for both create and edit)
│   └── student-form.component.html
├── models/
│   ├── student.model.ts
│   └── student-filter.model.ts
└── services/
    └── student.service.ts
```

### Key UI Behaviours

- List shows `currentMonthlyValue`; if it differs from `agreedMonthlyValue`,
  show the original with strikethrough and a "Discount" badge
- Guardian fields appear only when `birthDate` indicates a minor
- Financial status badge color: green = paid, yellow = pending, red = overdue
- Notes field is a textarea, max 500 chars
- Soft-deleted students do not appear in any list
