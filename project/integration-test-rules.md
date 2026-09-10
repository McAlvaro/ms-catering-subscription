# Integration Testing Rules — ms-catering-subscription

> Integration testing harness for the Catering Subscription microservice.
> Applies exclusively to the `infrastructure/` module.
> These rules complement `unit-testing-rules.md` and override section 3.3 (infrastructure layer).

---

## 0. Architecture — What Is Really Running

Integration tests exercise the **full vertical slice** of the application.
Nothing is mocked. Every layer is real, backed by H2 in-memory.

```
MockMvc  (HTTP client — simulates the real servlet stack)
    │
    │  HTTP request  (POST / GET / PATCH …)
    ▼
Controller  (real — from infrastructure/api/)
    │
    │  pipeline.send(command)
    ▼
Pipelinr Pipeline  (real — an.awesome.pipelinr)
    │
    │  middleware chain
    ▼
TransactionalMiddleware  (real — wraps everything in @Transactional)
    │
    ▼
Command / Query Handler  (real — from application/)
    │
    │  calls domain services → aggregate → repository
    ▼
Repository / Domain Services  (real — H2 in-memory, MySQL mode)
    │
    ▼
H2 Database  (Liquibase migrations applied on startup)
```

### Key differences from unit and slice tests

| Aspect | Unit test (`*Test`) | Integration test (`*IT`) |
|---|---|---|
| Spring context | ❌ Not started | ✅ Full context (`@SpringBootTest`) |
| Pipeline | ❌ Not involved | ✅ Real Pipelinr pipeline |
| `TransactionalMiddleware` | ❌ Not involved | ✅ Real (commits within test `@Transactional`) |
| Handler | Direct call or `@InjectMocks` | ✅ Resolved by Spring via `ApplicationBeansConfig` |
| Database | ❌ Mocked repository | ✅ H2 in-memory with Liquibase |
| HTTP layer | ❌ No servlet | ✅ Real `DispatcherServlet` + `GlobalExceptionHandler` |

### Why `@Transactional` on the test class AND `TransactionalMiddleware` both work

`@Transactional` on `BaseIntegrationTest` opens a transaction for the **test method**.
`TransactionalMiddleware` (called during `pipeline.send(command)`) participates in
that same transaction — it does not open a new one because the propagation is `REQUIRED`
by default. This means:

- Data written in `@BeforeEach` is visible inside the controller call.
- All writes performed by the handler are visible to subsequent `mockMvc.perform()`
  calls within the same test (e.g. the duplicate-subscription scenario).
- Everything is rolled back automatically after the test method completes.

---

## 1. Testing Stack

| Tool | Version | Purpose |
|---|---|---|
| **JUnit Jupiter** | 5.x (via Spring Boot parent) | Test execution engine |
| **Spring Boot Test** | 4.x (`@SpringBootTest`) | Full application context with H2 |
| **MockMvc** | (via `@AutoConfigureMockMvc`) | HTTP request simulation against the real servlet stack |
| **H2** | In-memory, MySQL mode | Replaces the real MySQL database |
| **Liquibase** | (via Spring Boot auto-config) | Applies real migrations to H2 on startup |
| **Jackson** | (via Spring Boot parent) | JSON serialization of commands and deserialization of responses |
| **Maven Failsafe** | 3.2.5 | Executes `*IT.java` classes in the `integration-test` phase |

---

## 2. File Structure and Naming

### 2.1 File location

Integration test files mirror the package of the controller under test, inside
`infrastructure/src/test/java/`:

```
infrastructure/src/
├── main/java/com/mcalvaro/mscatering/infrastructure/api/subscription/
│   └── SubscriptionController.java
└── test/java/com/mcalvaro/mscatering/infrastructure/api/subscription/
    └── CreateSubscriptionApiIT.java   <- same package, IT suffix
```

### 2.2 File naming convention

```
<UseCase>ApiIT.java

Examples:
  CreateSubscriptionApiIT.java
  PauseSubscriptionApiIT.java
  SavePatientReferenceApiIT.java
```

Each file covers **one endpoint** (one HTTP method + one URL). Multiple use cases
on the same controller each get their own `*IT.java` file.

### 2.3 Class declaration

Test classes are **package-private** (no access modifier), as in JUnit 5 unit tests:

```java
// Correct
class CreateSubscriptionApiIT extends BaseIntegrationTest { ... }

// Incorrect
public class CreateSubscriptionApiIT extends BaseIntegrationTest { ... }
```

---

## 3. Base Class — `BaseIntegrationTest`

Every integration test class **must** extend `BaseIntegrationTest`. Direct use
of `@SpringBootTest` / `@AutoConfigureMockMvc` in a test class is forbidden.

`BaseIntegrationTest` provides:

| Member | Type | Description |
|---|---|---|
| `mockMvc` | `MockMvc` | Pre-configured HTTP client for the full servlet stack |
| `objectMapper` | `ObjectMapper` | Jackson mapper with `JavaTimeModule`; `LocalDate`, `LocalTime`, `Instant` are serialized as ISO strings |

The class is annotated with:

```java
@SpringBootTest(classes = MsCateringApplication.class)  // full context
@AutoConfigureMockMvc                                   // real servlet stack
@ActiveProfiles("test")                                 // activates application-test.yml
@Transactional                                          // rolls back after each test
```

> **Why `@Transactional` at the class level?**
> It guarantees that any state written to H2 during a test (e.g. a `patientRepository.save()`)
> is rolled back automatically, keeping tests isolated from each other.
> The `TransactionalMiddleware` pipeline commits within the same transaction,
> so the controller-under-test can see the `@BeforeEach` data.

---

## 4. Infrastructure Setup Rules

| Rule | Description |
|---|---|
| **RI-01** | Every IT class must extend `BaseIntegrationTest`. Never use `@SpringBootTest` directly. |
| **RI-02** | Database state required by a test must be set up in `@BeforeEach` via real repository beans (e.g. `IPatientReferenceRepository`). Never use raw SQL or `TestEntityManager` unless the fixture cannot be expressed through the domain API. |
| **RI-03** | The endpoint URL must be stored in a `private static final String URL` constant at the top of the class. |
| **RI-04** | The `application-test.yml` profile must be the sole source of database configuration for tests. No inline `@TestPropertySource` overrides for database settings. |
| **RI-05** | Do not add `@MockBean` for infrastructure services in IT tests. The real beans must run end-to-end. |

---

## 5. Test Structure Rules

### 5.1 Nested classes for test grouping

Tests **must** be organized with `@Nested` classes, one per logical scenario group:

```java
@Nested
@DisplayName("Happy Path")
class HappyPath { ... }

@Nested
@DisplayName("Domain Rule Violations")
class DomainRuleViolations { ... }

@Nested
@DisplayName("Malformed Payload")
class MalformedPayload { ... }
```

The three mandatory groups for any write endpoint are:

| Group | Scenarios |
|---|---|
| **Happy Path** | All valid inputs that result in 2xx. Cover one test per allowed variant (e.g. 15-day plan vs 30-day plan). |
| **Domain Rule Violations** | One test per `DomainException` code that the use case can throw. |
| **Malformed Payload** | Invalid JSON structure, unknown enum values, wrong date formats, empty body. |

### 5.2 Method naming

Same convention as unit tests:

```
should[ExpectedResult]When[Condition]

Examples:
  shouldReturn201WithUuidWhenCommandIsValid()
  shouldReturn400WhenPatientNotFound()
  shouldReturn400WhenPlanDurationIsInvalid()
  shouldReturn400WhenStartDateHasInvalidFormat()
```

### 5.3 `@DisplayName` annotation

- **Required** on every `@Test` method and every `@Nested` class.
- **Language:** English.
- Include the HTTP status code and the domain error code when applicable:

```java
@DisplayName("Should return 400 SUB-014 when patient does not exist in the local read model")
```

### 5.4 AAA pattern

Every test must use the AAA (Arrange-Act-Assert) comment structure.
When Act and Assert are combined in a single `mockMvc.perform(...).andExpect(...)` chain,
use `// Act & Assert`:

```java
@Test
@DisplayName("Should return 201 Created with a UUID body when the command is valid (15-day plan)")
void shouldReturn201WithUuidWhenCommandIsValid() throws Exception {
    // Arrange
    String body = objectMapper.writeValueAsString(buildValidCommand(activePatientId, 15));

    // Act & Assert
    mockMvc.perform(post(URL)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
            .andExpect(status().isCreated());
}
```

---

## 6. Assertion Rules

| Rule | Description |
|---|---|
| **RA-01** | Always assert the HTTP status code (`.andExpect(status().isCreated())`, `.andExpect(status().isBadRequest())`, etc.). |
| **RA-02** | For domain error responses, always assert `$.code` with the exact domain error code (e.g. `"SUB-014"`): `.andExpect(jsonPath("$.code").value("SUB-014"))`. |
| **RA-03** | For 2xx responses that return a resource ID, assert the response is not empty or assert the UUID format when the endpoint returns the created ID. |
| **RA-04** | Do not assert `$.message` unless the test specifically documents a message contract. Error messages are for humans and may change; error codes are the stable API contract. |
| **RA-05** | `andDo(print())` is **forbidden** in committed tests. Use it only locally for debugging, then remove it. |

---

## 7. Error Code Coverage — Mandatory Scenarios

For any write endpoint backed by a command handler, the following scenarios **must** be covered:

### 7.1 Domain violations (→ `DomainException` → 400)

One dedicated test per `DomainException` code the handler can throw, including:

- Every code thrown by `SubscriptionDuplicationValidator` or any domain service called first
- Every code thrown by Value Object constructors called inside the handler
- Every code thrown by the Aggregate Root's factory method or state-transition methods

> **How to identify codes:** Read `SubscriptionErrors`, each VO constructor, and the
> handler itself. Every `new DomainException("XX-000", ...)` is a test case.

### 7.2 Enum / format violations (→ `IllegalArgumentException` or `HttpMessageNotReadableException` → 400)

| Scenario | Exception type | Expected `$.code` |
|---|---|---|
| Field declared as `String` in command + `Enum.valueOf()` in handler | `IllegalArgumentException` | `BAD_REQUEST` |
| Field declared as `EnumType` in command (Jackson deserializes) | `HttpMessageNotReadableException` | `INVALID_PAYLOAD` |
| Date/time field with wrong format | `HttpMessageNotReadableException` | `INVALID_PAYLOAD` |

> **Key distinction:** If `serviceType` is a `String` in the command record, Jackson
> accepts any value. The enum conversion happens inside the handler, throwing
> `IllegalArgumentException` → `BAD_REQUEST`. If the field were typed directly as
> `ServiceType`, Jackson would fail on deserialization → `INVALID_PAYLOAD`.

---

## 8. Fixture Helpers

| Rule | Description |
|---|---|
| **RF-01** | Fixture helpers (`buildValidCommand(...)`, `buildValidCommandJson(...)`) are declared `private` at the **bottom** of the test class, after all `@Nested` groups. |
| **RF-02** | A `buildValidCommand(UUID patientId, int planDays)` helper that returns a fully valid command is **required** in every IT class for a write endpoint. |
| **RF-03** | Invalid scenarios are built by passing a specific bad argument to the constructor (preferred) or by post-processing the JSON string with `.replace(...)` only when the field cannot hold the invalid value in the Java type system (e.g. replacing a valid enum string with an unknown one). |
| **RF-04** | Never hard-code UUIDs for patients or other entities. Generate them with `UUID.randomUUID()` in `@BeforeEach` and store them as instance fields. |

```java
// Correct — invalid value injected at the Java level
new CreateSubscriptionCommand(..., BigDecimal.ZERO, ...);

// Acceptable — invalid value only expressible as raw JSON
body.replace("\"LUNCH\"", "\"DESAYUNO\"");
```

---

## 9. Maven Lifecycle

Integration tests run in the `integration-test` phase via the **Failsafe** plugin.
They are **not** executed by `mvn test`.

```bash
# Run all tests (unit + integration) and verify
mvn verify

# Run only the infrastructure module integration tests
mvn -pl infrastructure verify

# Run only unit tests (skips IT classes)
mvn test
```

Files named `*IT.java` are picked up automatically by Failsafe.
Do **not** rename them to `*Test.java` — they would then run under Surefire without
the full Spring context and would fail.

---

## 10. Global Prohibitions

| Forbidden | Alternative |
|---|---|
| `@SpringBootTest` directly on an IT class | Extend `BaseIntegrationTest` |
| `@MockBean` for domain or application services in IT tests | Let the real bean run; set up state via `@BeforeEach` |
| `andDo(print())` in committed code | Remove after local debugging |
| Hard-coded UUIDs for test entities | `UUID.randomUUID()` in `@BeforeEach` |
| `Thread.sleep()` | Not applicable — MockMvc is synchronous |
| Asserting `$.message` as the primary correctness check | Assert `$.code` instead |
| `*Test.java` suffix for full-context tests | Use `*IT.java` so Failsafe picks them up |
| `@Disabled` without an explanatory comment | Leave a `// TODO` with reason and date |
