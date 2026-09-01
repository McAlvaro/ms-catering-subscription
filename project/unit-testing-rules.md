# Unit Testing Rules — ms-catering-subscription

> Unit testing harness for the Catering Subscription microservice.
> Based on Clean Architecture + DDD. Applies to all modules: `domain`, `application`, and `infrastructure`.

---

## 1. Testing Stack

| Tool | Version | Purpose |
|---|---|---|
| **JUnit Jupiter** | 5.x (via Spring Boot parent) | Test execution engine |
| **AssertJ** | 3.x (via Spring Boot parent) | Fluent and expressive assertions |
| **Mockito** | 5.x (via `mockito-junit-jupiter`) | Mock creation and interaction verification |
| **JaCoCo** | 0.8.12 | Code coverage measurement and HTML report |
| **Maven Surefire** | 3.2.5 | Test execution within the Maven lifecycle |
| **H2** | Runtime (via Spring Boot parent) | In-memory database for infrastructure tests |
| **Spring Boot Test** | 4.x | Context and MockMvc for integration tests in `infrastructure` |

---

## 2. File Structure

Tests **mirror** the source code structure. The test folder must replicate the exact same package as the class being tested.

```
src/
├── main/java/com/mcalvaro/mscatering/
│   └── domain/subscription/vo/ValidityPeriod.java
└── test/java/com/mcalvaro/mscatering/
    └── domain/subscription/vo/ValidityPeriodTest.java   <- same package
```

**Naming convention:** `<ClassName>Test.java`

---

## 3. Rules by Layer

### 3.1 `domain/` Layer

> Pure business logic. **No Spring, no Mockito** (except fixtures). JUnit 5 + AssertJ only.

| Rule | Description |
|---|---|
| **R-D01** | Value Object tests must verify each business invariant with a dedicated test. |
| **R-D02** | Use `@ParameterizedTest` with `@NullAndEmptySource` + `@ValueSource` to cover null, empty, and blank values in a single test. |
| **R-D03** | Verify the error code (`code`) of `DomainException` with `.hasFieldOrPropertyWithValue("code", "XX-000")`. |
| **R-D04** | Verify value equality of Records with `.isEqualTo()` — never use `==`. |
| **R-D05** | Aggregate Root tests must cover: creation (`create`), state transitions, and domain invariant exceptions. |
| **R-D06** | Using `mock()` on domain classes in this layer is forbidden. Domain objects must be used as real instances. |

**Reference example:** `domain/src/test/java/.../subscription/vo/ValidityPeriodTest.java`

---

### 3.2 `application/` Layer

> Use cases / Command Handlers. Every external dependency is **mocked**. Spring context is never started.

| Rule | Description |
|---|---|
| **R-A01** | Every test class must be annotated with `@ExtendWith(MockitoExtension.class)`. |
| **R-A02** | Repository interfaces and domain services are declared with `@Mock`. |
| **R-A03** | The class under test is declared with `@InjectMocks` (Mockito injects the constructor). |
| **R-A04** | Use `when(...).thenReturn(...)` (stubbing) to simulate collaborator responses. |
| **R-A05** | Use `verify(mock, times(N)).method(arg)` to confirm the handler called its collaborators. |
| **R-A06** | Use `verify(mock, never()).method(any())` to confirm nothing is persisted on error. |
| **R-A07** | Do not call `new` on repository or infrastructure service implementations. |
| **R-A08** | Fixture helpers (`buildXxx()`) are declared `private` at the bottom of the class. |
| **R-A09** | `mock()` is allowed in fixtures only if the test does not verify the internal state of that object. |

**Minimum scenarios to cover in a Command Handler:**

1. ✅ **Happy Path** — the command executes successfully and returns the expected result.
2. 🔍 **Contract verification** — the correct arguments are passed to the collaborators.
3. ❌ **Error propagation** — if the domain throws an exception, the repository is never called.

**Reference example:** `application/src/test/java/.../CloseConsolidatedCalendarCommandHandlerTest.java`

---

### 3.3 `infrastructure/` Layer

> REST controllers, mappers, JPA repositories. Spring Boot Test + H2 are used when context is required.

| Rule | Description |
|---|---|
| **R-I01** | REST controller tests use `@WebMvcTest(XController.class)` + `MockMvc`. Full `@SpringBootTest` is never used. |
| **R-I02** | Application use cases are mocked with `@MockBean` in controller tests. |
| **R-I03** | JPA repository tests use `@DataJpaTest` with H2 in memory. An active MySQL instance is never required. |
| **R-I04** | Mapper/Converter tests are pure unit tests: no Spring, no Mockito. |
| **R-I05** | The test database must use `spring.jpa.hibernate.ddl-auto=create-drop` or a separate Liquibase script. |

---

## 4. Code Conventions

### 4.1 Method naming

```
should[ExpectedResult]When[Condition]

Examples:
  shouldCreateValidityPeriodSuccessfullyWhenDatesAreValid()
  shouldThrowDomainExceptionWhenStreetIsBlank()
  shouldPropagateExceptionWhenCalendarHasNoLines()
```

### 4.2 @DisplayName annotation

- **Required** on every test method.
- **Language:** English.
- **Format:** a complete sentence describing the expected behavior.

```java
@DisplayName("Should throw DomainException VO-003 when street is null, empty or blank")
```

### 4.3 Meaningful assertions

Every assertion must verify real system behavior. Trivial or tautological assertions add no value and give a false sense of coverage.

```java
// Forbidden — always passes, verifies nothing
assertThat(true).isTrue();
assertTrue(true);

// Correct — verifies the actual state of the object under test
assertThat(calendar.getStatus()).isEqualTo(ConsolidateStatus.CLOSED);
assertThat(returnedId).isEqualTo(calendar.getId());
```

### 4.4 Internal structure: AAA (Arrange-Act-Assert)

Every test must visually separate its three phases with comments:

```java
// Arrange  <- prepare data and stubs
// Act      <- execute the action under test
// Assert   <- verify the result

// Note: if Act and Assert happen together, use:
// Act & Assert
```

Capturing exceptions with an empty or assertionless `try/catch` is **forbidden**. If an exception is expected, use `assertThatThrownBy()`:

```java
// Forbidden — the test always passes even if the wrong exception is thrown
try {
    handler.handle(command);
} catch (Exception e) { }

// Correct — verifies exception type and message
assertThatThrownBy(() -> handler.handle(command))
        .isInstanceOf(DomainException.class)
        .hasFieldOrPropertyWithValue("code", "CAL-002");
```

### 4.5 Test class visibility

Test classes are declared **without an access modifier** (package-private), not `public`.

```java
// Correct
class ValidityPeriodTest { ... }

// Incorrect (unnecessary in JUnit 5)
public class ValidityPeriodTest { ... }
```

---

## 5. Code Coverage — Targets

| Module | Lines | Branches |
|---|---|---|
| `domain` | >= 80% | >= 75% |
| `application` | >= 70% | >= 60% |
| `infrastructure` | >= 50% | >= 40% |

### Maven commands

```bash
# Run all tests and generate JaCoCo reports
mvn test

# Run only the domain module
mvn -pl domain test

# Run only the application module
mvn -pl application test -am

# Open coverage report (in your browser)
# domain/target/site/jacoco/index.html
# application/target/site/jacoco/index.html
```

---

## 6. Global Prohibitions

| Forbidden | Alternative |
|---|---|
| `@SpringBootTest` in `domain` or `application` tests | Pure unit tests without context |
| Live MySQL connection in tests | H2 in memory with `@DataJpaTest` |
| `Thread.sleep()` in tests | Inject a mocked `Clock` |
| `@Disabled` without an explanatory comment | Leave a TODO with the reason and date |
| `System.out.println` in tests | Use the Surefire / JaCoCo report |
| `assertTrue(true)` or `assertThat(true).isTrue()` | Assertion on the real state of the object under test |
| Empty or assertionless `try/catch` to capture exceptions | `assertThatThrownBy(() -> ...).isInstanceOf(...)` |
