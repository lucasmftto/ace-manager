# Ace Manager — Project Foundation

This file is the base reference for all Claude Code work on this project.
Every module spec extends and follows the rules defined here.

---

## Project Overview

**Ace Manager** is a tennis academy management system. It replaces a manual
spreadsheet and centralizes student management, teacher scheduling, class
attendance, billing, teacher payouts, operational expenses, and cash flow.

The key business differentiator is **parametrized teacher payout per
teacher-student pair**: each teacher can have a different percentage or
hourly rate for each student they teach.

---

## Modules

| # | Module                  | Description                                           |
|---|-------------------------|-------------------------------------------------------|
| 1 | Dashboard               | Daily overview: classes, financials, alerts           |
| 2 | Students                | Registration, agreed vs. current value, payment method|
| 3 | Teachers                | Registration, payout model, per-student configuration |
| 4 | Plans & Packages        | Subscription, bundle, drop-in, group class types      |
| 5 | Class Management        | Weekly fixed schedule, attendance tracking            |
| 6 | Student Financials      | Charges, payments, overdue control                    |
| 7 | Teacher Payouts         | Commission + extras (credit/debit), monthly closing   |
| 8 | Operational Expenses    | Fixed and variable academy costs                      |
| 9 | Cash Flow               | Monthly P&L: revenue − payouts − expenses             |

---

## Tech Stack

### Backend
- **Java 21** (use records, sealed classes, pattern matching where appropriate)
- **Spring Boot 3.x**
- **Spring Data JPA** with Hibernate
- **Spring Security** with JWT authentication
- **PostgreSQL** (production and tests via Testcontainers)
- **Flyway** for database migrations
- **Maven** for build

### Frontend
- **Angular 17+** with standalone components
- **Angular Material** for UI components
- **TypeScript** — strict mode enabled
- **RxJS** for reactive patterns
- **Angular Router** with lazy-loaded feature modules

### Infrastructure
- **Docker + Docker Compose** for local environment
- **Testcontainers** for integration tests (PostgreSQL)
- **JUnit 5** + **AssertJ** for test assertions
- **REST Assured** or **MockMvc** for HTTP-layer testing

---

## Language Rules

> All code — backend and frontend — must be written in English.

This applies to:
- Class names, method names, field names, variable names
- Database column names and table names (via Flyway migrations)
- REST endpoint paths and JSON field names
- Angular component names, service names, template variables
- Git commit messages and code comments

The only exception is user-facing UI text (labels, messages, alerts),
which should be in Portuguese (pt-BR) since the end users are Brazilian.

---

## Project Structure

### Backend (`ace-manager-api/`)

```
ace-manager-api/
├── src/main/java/com/acemanager/
│   ├── AceManagerApplication.java
│   ├── config/
│   │   ├── SecurityConfig.java
│   │   ├── JwtConfig.java
│   │   └── OpenApiConfig.java
│   ├── shared/
│   │   ├── exception/
│   │   │   ├── GlobalExceptionHandler.java
│   │   │   ├── ResourceNotFoundException.java
│   │   │   └── BusinessException.java
│   │   ├── dto/
│   │   │   └── PageResponse.java
│   │   └── audit/
│   │       └── Auditable.java
│   ├── auth/
│   │   ├── AuthController.java
│   │   ├── AuthService.java
│   │   └── dto/
│   ├── student/
│   │   ├── Student.java
│   │   ├── StudentRepository.java
│   │   ├── StudentService.java
│   │   ├── StudentController.java
│   │   └── dto/
│   ├── teacher/
│   ├── plan/
│   ├── classschedule/
│   ├── studentfinancial/
│   ├── teacherpayout/
│   ├── expense/
│   └── cashflow/
├── src/main/resources/
│   ├── application.yml
│   ├── application-test.yml
│   └── db/migration/
│       └── V1__initial_schema.sql
└── src/test/java/com/acemanager/
    ├── student/
    │   └── StudentControllerTest.java
    ├── teacher/
    └── ...
```

### Frontend (`ace-manager-web/`)

```
ace-manager-web/
├── src/
│   ├── app/
│   │   ├── core/
│   │   │   ├── auth/
│   │   │   ├── guards/
│   │   │   ├── interceptors/
│   │   │   └── services/
│   │   ├── shared/
│   │   │   ├── components/
│   │   │   └── models/
│   │   ├── features/
│   │   │   ├── dashboard/
│   │   │   ├── students/
│   │   │   ├── teachers/
│   │   │   ├── plans/
│   │   │   ├── classes/
│   │   │   ├── financials/
│   │   │   ├── payouts/
│   │   │   ├── expenses/
│   │   │   └── cashflow/
│   │   ├── app.routes.ts
│   │   └── app.config.ts
│   ├── environments/
│   └── styles/
```

---

## Architecture Patterns

### Backend

**Controller → Service → Repository** layering is mandatory. No business logic
in controllers; no repository calls in controllers.

```java
// Controller: HTTP concern only
@RestController
@RequestMapping("/api/v1/students")
@RequiredArgsConstructor
public class StudentController {
    private final StudentService studentService;

    @GetMapping
    public ResponseEntity<Page<StudentSummaryResponse>> findAll(Pageable pageable) {
        return ResponseEntity.ok(studentService.findAll(pageable));
    }
}

// Service: business logic
@Service
@Transactional
@RequiredArgsConstructor
public class StudentService {
    private final StudentRepository studentRepository;
    // business logic here
}
```

**DTOs** are mandatory — never expose JPA entities directly in responses.
Use separate request/response DTOs per operation.

```java
// Request DTOs: validation annotations
public record CreateStudentRequest(
    @NotBlank String name,
    @NotBlank String phone,
    @Email String email,
    @NotNull PaymentMethod preferredPaymentMethod
) {}

// Response DTOs: what the API returns
public record StudentResponse(
    UUID id,
    String name,
    String phone,
    String email,
    BigDecimal agreedMonthlyValue,
    BigDecimal currentMonthlyValue,
    PaymentMethod preferredPaymentMethod,
    StudentStatus status
) {}
```

**Enums** for fixed-value fields:
```java
public enum PaymentMethod { PIX, BOLETO, PIX_AUTOMATIC, OTHER }
public enum StudentStatus { ACTIVE, INACTIVE, SUSPENDED }
public enum PayoutModel { PERCENTAGE, HOURLY_RATE }
public enum ChargeStatus { PENDING, PAID, OVERDUE }
public enum ExpenseStatus { PENDING, PAID }
```

**Global exception handler** for consistent error responses:
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse(ex.getMessage()));
    }
}
```

### Frontend

**Feature modules** are lazy-loaded via Angular Router.
Each feature has its own: component, service, model, and routes file.

```typescript
// Lazy loading in app.routes.ts
{
  path: 'students',
  loadChildren: () =>
    import('./features/students/students.routes').then(m => m.STUDENTS_ROUTES)
}
```

**Services** use `HttpClient` with typed responses:
```typescript
@Injectable({ providedIn: 'root' })
export class StudentService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/v1/students';

  findAll(params?: StudentFilterParams): Observable<Page<StudentSummary>> {
    return this.http.get<Page<StudentSummary>>(this.baseUrl, { params });
  }
}
```

---

## Database Conventions

- All table names: **snake_case**, plural (e.g., `students`, `teacher_payouts`)
- All column names: **snake_case**
- Primary keys: `UUID` type, generated with `gen_random_uuid()`
- Timestamps: `created_at` and `updated_at` on every table (managed by `Auditable`)
- Foreign keys: `{referenced_table_singular}_id` (e.g., `student_id`, `teacher_id`)
- Soft delete: `deleted_at TIMESTAMP` column (null = active)
- Migrations: numbered `V{n}__{description}.sql` via Flyway

```sql
-- Example table pattern
CREATE TABLE students (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(255) NOT NULL,
    phone       VARCHAR(20),
    email       VARCHAR(255),
    status      VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP   NOT NULL DEFAULT NOW(),
    deleted_at  TIMESTAMP
);
```

---

## Testing Strategy

### Rules

1. **Tests start from the Controller layer** — no service-only unit tests
2. **No mocking of services or repositories** — real Spring context + real DB
3. **Testcontainers** provides a real PostgreSQL instance for all tests
4. **Each test is isolated** — use `@Transactional` or manual cleanup
5. Test method naming: `should{ExpectedBehavior}_when{Condition}`

### Testcontainers Base Class

Every test class extends this:

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
public abstract class BaseIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("acemanager_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
}
```

### Test Example Pattern

```java
class StudentControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        studentRepository.deleteAll();
    }

    @Test
    void shouldCreateStudent_whenValidRequest() throws Exception {
        var request = new CreateStudentRequest(
            "Marcos Lima",
            "(11) 99876-5432",
            "marcos@email.com",
            PaymentMethod.PIX
        );

        mockMvc.perform(post("/api/v1/students")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Marcos Lima"))
            .andExpect(jsonPath("$.status").value("ACTIVE"));

        assertThat(studentRepository.count()).isEqualTo(1);
    }

    @Test
    void shouldReturnNotFound_whenStudentDoesNotExist() throws Exception {
        var nonExistentId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/students/{id}", nonExistentId))
            .andExpect(status().isNotFound());
    }
}
```

---

## API Conventions

- Base path: `/api/v1/`
- JSON responses for all endpoints
- Pagination: Spring's `Pageable` (params: `page`, `size`, `sort`)
- Date format: ISO 8601 (`yyyy-MM-dd`, `yyyy-MM-dd'T'HH:mm:ss`)
- Monetary values: `BigDecimal`, never `double` or `float`
- IDs: `UUID` everywhere

### Standard HTTP verbs
| Action          | Method | Path                    |
|-----------------|--------|-------------------------|
| List (paginated)| GET    | `/resource`             |
| Get one         | GET    | `/resource/{id}`        |
| Create          | POST   | `/resource`             |
| Full update     | PUT    | `/resource/{id}`        |
| Partial update  | PATCH  | `/resource/{id}`        |
| Delete (soft)   | DELETE | `/resource/{id}`        |

---

## Security

- JWT-based authentication
- Three roles: `ROLE_OWNER`, `ROLE_TEACHER`, `ROLE_STUDENT`
- Each endpoint annotated with `@PreAuthorize`
- Tokens expire in 8 hours; refresh token valid for 7 days
- Passwords hashed with BCrypt

---

## Docker Compose (local)

```yaml
# docker-compose.yml
services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: acemanager
      POSTGRES_USER: acemanager
      POSTGRES_PASSWORD: acemanager
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

  api:
    build: ./ace-manager-api
    ports:
      - "8080:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/acemanager
      SPRING_DATASOURCE_USERNAME: acemanager
      SPRING_DATASOURCE_PASSWORD: acemanager
    depends_on:
      - postgres

volumes:
  postgres_data:
```

---

## application-test.yml

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
  flyway:
    enabled: true
logging:
  level:
    com.acemanager: DEBUG
```
