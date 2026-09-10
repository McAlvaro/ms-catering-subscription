---
name: integration-test-writer
description: >-
  Generates JUnit 5 integration tests (*IT.java) for a given controller class
  or endpoint directory in the infrastructure module, strictly following the
  project's integration-test-rules.md harness. Before generating, it analyses
  each target class to decide whether an integration test is feasible or not.
  Compatible with Antigravity, OpenCode, Claude, and Codex.
---

# integration-test-writer

## Purpose

Generate `*IT.java` integration test files for REST controllers in this project,
following the single source of truth: `project/integration-test-rules.md`.

Works for:
- A **single controller class**: e.g., `infrastructure/.../SubscriptionController.java`
- A **directory**: e.g., `infrastructure/.../api/` — analyses all controllers found
  and generates integration tests for the eligible ones.

---

## Step 1 — Read the Rules (Source of Truth)

Before writing any test, **read the rules file directly**:

```
project/integration-test-rules.md
```

Extract the following from the rules file:

- **Section 0** – Architecture diagram: the real call stack (MockMvc → Controller →
  Pipelinr Pipeline → TransactionalMiddleware → Handler → Repository → H2).
- **Section 1** – Testing stack: tools and their versions.
- **Section 2** – File structure and naming conventions (`<UseCase>ApiIT.java`).
- **Section 3** – `BaseIntegrationTest`: what it provides and why `@Transactional` works.
- **Sections 4–8** – Infrastructure rules (RI-*), test structure rules, assertion rules
  (RA-*), error code coverage, and fixture helper rules (RF-*).
- **Section 9** – Maven lifecycle: how Failsafe picks up `*IT.java`.
- **Section 10** – Global prohibitions.

> **Do not hardcode or assume rules.** Always adhere to the active definitions in
> `project/integration-test-rules.md`.

---

## Step 2 — Eligibility Analysis (Run Before Generating Anything)

For each target file or directory, analyse the source classes and classify each one
before generating any test. Print the eligibility report to the user.

### How to classify a class

Read the class and check the following criteria:

| Criterion | Eligible (✅) | Not eligible (❌) |
|---|---|---|
| Is it a `@RestController` or `@Controller`? | Yes | No — skip |
| Does it delegate to a `Pipeline` (Pipelinr)? | Yes | No (directly calls service) — warn |
| Does a corresponding Command/Query Handler exist in `application/`? | Yes | No — skip |
| Is there a registered repository implementation in `infrastructure/`? | Yes | No — skip |

### Classes that are NOT eligible for integration tests

| Class type | Reason | Recommended alternative |
|---|---|---|
| Domain VO / Aggregate / Entity | No HTTP layer, no Spring context | `unit-test-writer` skill |
| Command / Query Handler (application) | No HTTP layer, no servlet | `unit-test-writer` skill |
| Domain Service | No HTTP layer, no Spring context | `unit-test-writer` skill |
| Repository implementation (`@Repository`) | No HTTP layer | `@DataJpaTest` slice test (separate) |
| Configuration class (`@Configuration`) | Not an HTTP endpoint | Manual or Spring integration test |
| Middleware / Interceptor | Not a controller | Manual test |

### Eligibility report format

Output this report **before writing any file**:

```
Eligibility Analysis
────────────────────
✅ SubscriptionController          → ELIGIBLE   (3 endpoints detected, 3 *IT files to generate)
✅ PatientController               → ELIGIBLE   (1 endpoint detected, 1 *IT file to generate)
❌ GlobalExceptionHandler          → NOT ELIGIBLE (no endpoint — @ControllerAdvice, not a controller)
❌ ApplicationBeansConfig          → NOT ELIGIBLE (configuration class, no HTTP endpoint)
❌ DefaultSubscriptionDuplicationValidator → NOT ELIGIBLE (domain service — use unit-test-writer)

Generating IT files for: SubscriptionController, PatientController
```

> **HALT condition:** If **zero** classes in the target are eligible, stop immediately.
> Do not generate any file. Report to the user:
>
> ```
> ⛔ No eligible controllers found in the target path.
> Integration tests can only be generated for @RestController / @Controller classes
> that delegate to a Pipelinr Pipeline.
> Consider using the unit-test-writer skill for domain or application classes.
> ```

---

## Step 3 — Discover Scenarios From the Source Code

For each **eligible** controller, read:

1. **The controller method** — HTTP method, URL, request body type.
2. **The corresponding Command record** — all fields and their types.
3. **The Command Handler** — which domain services and repositories it calls.
4. **All Value Object constructors** called by the handler — extract every
   `DomainException` code they can throw.
5. **Domain error catalog** (e.g., `SubscriptionErrors.java`) — all codes the
   handler's domain services can throw.
6. **`GlobalExceptionHandler`** — how each exception type is mapped to an HTTP
   status and `$.code`.

Build a scenario matrix like this before writing code:

```
Scenario Matrix — POST /api/subscriptions
──────────────────────────────────────────
Happy Path
  ✅ Valid 15-day plan          → 201 Created

Domain Rule Violations (DomainException → 400)
  ❌ SUB-014  Patient not found
  ❌ SUB-015  Patient inactive
  ❌ SUB-013  Duplicate subscription (INV-01)
  ❌ SUB-005  Invalid plan duration
  ❌ SUB-006  Price ≤ 0  (×2: zero, negative)
  ❌ VO-008   startDate not strictly before endDate
  ❌ VO-002   TimeWindow invalid (end ≤ start)
  ❌ VO-003   prefStreet blank
  ❌ VO-004   prefCity blank

Malformed Payload (Jackson / IllegalArgumentException → 400)
  ❌ BAD_REQUEST    serviceType string not in enum (handler throws IAE)
  ❌ INVALID_PAYLOAD  date field with wrong format (Jackson fails)
  ❌ 400 (any)      Empty JSON body {}
```

> Note: If `serviceType` is a `String` in the Command record, Jackson accepts any
> value and the enum conversion fails inside the handler with `IllegalArgumentException`
> → `BAD_REQUEST`. If it were typed as the enum directly, Jackson fails at
> deserialization → `INVALID_PAYLOAD`. Apply this distinction from the rules file.

---

## Step 4 — Write the `*IT.java` File

Use the scenario matrix and the rules from `project/integration-test-rules.md` to write
the test file. Adhere strictly to:

1. **One file per endpoint** (one use case per `*IT.java`).
2. **File location**: mirror the controller package under `infrastructure/src/test/java/`.
3. **Extends `BaseIntegrationTest`** — never add `@SpringBootTest` directly.
4. **`@Nested` groups**: `HappyPath`, `DomainRuleViolations`, `MalformedPayload`
   (at minimum; add more if needed).
5. **One `@Test` method per scenario** — no scenarios bundled in a single test.
6. **Method naming**: `should[ExpectedResult]When[Condition]()`.
7. **`@DisplayName`**: required on every `@Test` and every `@Nested` class.
   Include HTTP status code and domain error code in the display name when applicable.
8. **AAA structure**: explicit `// Arrange`, `// Act`, `// Assert` (or `// Act & Assert`).
9. **Assertion rules**:
   - Always assert HTTP status with `status().isCreated()` / `status().isBadRequest()`.
   - Always assert `$.code` with `jsonPath("$.code").value("SUB-014")` for error scenarios.
   - Never assert `$.message` as the primary correctness check.
10. **`@BeforeEach`** setup: create all required database entities via real repository
    beans (`@Autowired`), not raw SQL. Store IDs as `private UUID` instance fields.
11. **Fixture helpers**: `buildValidCommand(...)` and `buildValidCommandJson(...)` at
    the bottom of the class, declared `private`.
12. **Class visibility**: package-private (no `public` modifier).

---

## Step 5 — Verify Against the Prohibitions Checklist

Before finalising, check every generated file against the **Global Prohibitions**
in `project/integration-test-rules.md` (Section 10):

| Check | Prohibited pattern |
|---|---|
| No `@SpringBootTest` directly on the IT class | Must extend `BaseIntegrationTest` |
| No `@MockBean` for domain/application services | Real beans must run |
| No `andDo(print())` in committed code | Debug-only, must be removed |
| No hard-coded UUIDs for test entities | Use `UUID.randomUUID()` in `@BeforeEach` |
| No `Thread.sleep()` | Not applicable — MockMvc is synchronous |
| No `$.message` as primary assertion | Use `$.code` |
| No `*Test.java` suffix for IT files | Must end with `IT.java` |
| No `@Disabled` without explanatory comment | Add `// TODO` with reason and date |

Report any violation found and fix it before writing the final file.


