---
name: integration-test-verifier
description: >-
  Verifies existing JUnit 5 integration test files (*IT.java) against the rules
  defined in project/integration-test-rules.md. Reports a PASS/FAIL verdict per
  rule for a given test file or directory. Designed to run after
  integration-test-writer. Compatible with Antigravity, OpenCode, Claude, and Codex.
---

# integration-test-verifier

## Purpose

Audit one or more `*IT.java` files against the single source of truth:
`project/integration-test-rules.md`.
Produces a structured verdict report with **PASS** or **FAIL** for each rule
defined in the rules file.

Works for:
- A **single test file**: e.g., `infrastructure/src/test/.../CreateSubscriptionApiIT.java`
- A **directory**: e.g., `infrastructure/src/test/.../api/` — audits all `*IT.java` files found.

---

## Step 1 — Read the Rules (Source of Truth)

Before inspecting any test file, **read the rules file directly**:

```
project/integration-test-rules.md
```

Extract all active rules from the file grouped by section:

- **Section 2** – Naming and file structure rules.
- **Section 3** – `BaseIntegrationTest` usage requirements.
- **Section 4** – Infrastructure setup rules (RI-01 … RI-05).
- **Section 5** – Test structure rules: `@Nested` groups, method naming, `@DisplayName`, AAA.
- **Section 6** – Assertion rules (RA-01 … RA-05).
- **Section 7** – Error code coverage: mandatory scenarios per endpoint.
- **Section 8** – Fixture helper rules (RF-01 … RF-04).
- **Section 9** – Maven lifecycle (file must be `*IT.java`, not `*Test.java`).
- **Section 10** – Global prohibitions.

> **Do not hardcode or assume rules.** Always evaluate against the exact content
> read from `project/integration-test-rules.md`.

---

## Step 2 — Locate and Read the Target Test File(s)

1. If the input is a single file, read its content.
2. If the input is a folder, locate all `*IT.java` files recursively and read each one.
3. For each file, also read the **corresponding controller** and **command/query handler**
   to verify that the test covers all required scenarios (error code coverage — Section 7).

> **HALT condition:** If the input path contains **no `*IT.java` files**, stop immediately:
>
> ```
> ⛔ No *IT.java files found in the target path.
> Nothing to verify. Provide a path to an existing integration test file or directory.
> ```

---

## Step 3 — Audit Against `project/integration-test-rules.md`

For each file, evaluate every relevant rule from the rules file. Assign one of:

- **PASS** — The test complies with the rule.
- **FAIL** — The test violates the rule (record line number, offending code, and rule ID).
- **N/A** — The rule does not apply to this specific file.

### Rule evaluation checklist

#### File & naming (Section 2)
| Rule ID | What to check |
|---|---|
| **NM-01** | File name ends with `IT.java` (not `Test.java`). |
| **NM-02** | File name follows `<UseCase>ApiIT.java` convention. |
| **NM-03** | Test class package mirrors the controller's package under `src/test/java/`. |
| **NM-04** | Class is declared **package-private** (no `public` modifier). |

#### Base class (Section 3)
| Rule ID | What to check |
|---|---|
| **BC-01** | Class extends `BaseIntegrationTest`. |
| **BC-02** | No `@SpringBootTest`, `@AutoConfigureMockMvc`, or `@ActiveProfiles` declared directly on the IT class. |
| **BC-03** | No manual `MockMvc` or `ObjectMapper` field declarations (they come from `BaseIntegrationTest`). |

#### Infrastructure setup (Section 4 — RI-*)
| Rule ID | What to check |
|---|---|
| **RI-01** | Extends `BaseIntegrationTest` (same as BC-01; counts for both). |
| **RI-02** | `@BeforeEach` uses real repository beans (`@Autowired`), not raw SQL or `TestEntityManager`. |
| **RI-03** | Endpoint URL is stored in a `private static final String URL` constant. |
| **RI-04** | No inline `@TestPropertySource` overrides for database settings. |
| **RI-05** | No `@MockBean` for domain or application services. |

#### Test structure (Section 5)
| Rule ID | What to check |
|---|---|
| **TS-01** | Tests are organised in `@Nested` classes (at least `HappyPath` and `DomainRuleViolations`). |
| **TS-02** | Every `@Nested` class has a `@DisplayName`. |
| **TS-03** | Every `@Test` method has a `@DisplayName`. |
| **TS-04** | `@DisplayName` includes HTTP status code and domain error code for violation scenarios. |
| **TS-05** | Method name follows `should[ExpectedResult]When[Condition]()` convention. |
| **TS-06** | Every test has explicit `// Arrange`, `// Act`, `// Assert` (or `// Act & Assert`) comments. |
| **TS-07** | One scenario per `@Test` — no scenarios bundled together. |

#### Assertions (Section 6 — RA-*)
| Rule ID | What to check |
|---|---|
| **RA-01** | Every test asserts the HTTP status code. |
| **RA-02** | Every error scenario asserts `jsonPath("$.code").value(...)` with the exact domain code. |
| **RA-03** | 2xx scenarios assert the response is not empty or verify the returned resource ID. |
| **RA-04** | No test uses `$.message` as the primary correctness check. |
| **RA-05** | No `andDo(print())` in any test method. |

#### Error code coverage (Section 7)
| Rule ID | What to check |
|---|---|
| **EC-01** | There is at least one Happy Path test resulting in 2xx. |
| **EC-02** | Every `DomainException` code the handler can throw has a corresponding test. Cross-reference the handler, domain services called, and VO constructors. |
| **EC-03** | Malformed payload scenarios are covered (invalid enum value, wrong date format, empty body). |
| **EC-04** | The `BAD_REQUEST` vs `INVALID_PAYLOAD` distinction is applied correctly (see Section 7.2 of the rules). |

#### Fixture helpers (Section 8 — RF-*)
| Rule ID | What to check |
|---|---|
| **RF-01** | Fixture helpers (`buildValidCommand`, `buildValidCommandJson`) are declared `private` at the **bottom** of the class. |
| **RF-02** | A `buildValidCommand(UUID, int)` helper exists. |
| **RF-03** | Invalid scenarios use Java-level invalid values where possible; `.replace()` on JSON only when unavoidable. |
| **RF-04** | No hard-coded UUID literals for test entities — all use `UUID.randomUUID()` in `@BeforeEach`. |

#### Global prohibitions (Section 10)
| Rule ID | What to check |
|---|---|
| **GP-01** | No `@SpringBootTest` directly on the IT class. |
| **GP-02** | No `@MockBean` for domain/application services. |
| **GP-03** | No `andDo(print())`. |
| **GP-04** | No hard-coded UUID literals. |
| **GP-05** | No `Thread.sleep()`. |
| **GP-06** | No `@Disabled` without an explanatory comment. |
| **GP-07** | No `*Test.java` suffix — file must end in `IT.java`. |

---

## Step 4 — Generate the Verdict Report

Output a clear report per file using the following structure:

```markdown
## Audit: <relative path to IT file>
Controller under test: <ControllerClass>
Endpoint: <HTTP method> <URL>

| Rule ID | Rule Description | Status | Detail / Line |
|---|---|---|---|
| NM-04 | Class is package-private | PASS | |
| RI-05 | No @MockBean for domain/application services | PASS | |
| RA-02 | Error scenarios assert $.code | FAIL | Line 87: missing jsonPath("$.code") on shouldReturn400WhenPlanDurationIsInvalid |
| EC-02 | All DomainException codes covered | FAIL | SUB-006 (price ≤ 0) has no test |

Overall Verdict: FAIL (2 violations found)
```

If auditing multiple files or a directory, add a final summary:

```markdown
## Summary

| File | Violations | Overall |
|---|---|---|
| CreateSubscriptionApiIT.java | 0 | PASS |
| PatientApiIT.java | 1 | FAIL |

Total Files: 2 | Passed: 1 | Failed: 1
```

---

## Step 5 — Suggest Targeted Fixes

For each **FAIL**, output a minimal, actionable fix showing only the snippet to change:

```markdown
### Fix for RA-02 in CreateSubscriptionApiIT.java (Line 87)

**Problem:** Error scenario `shouldReturn400WhenPlanDurationIsInvalid` does not assert `$.code`.

**Current:**
```java
mockMvc.perform(post(URL).contentType(...).content(body))
        .andExpect(status().isBadRequest());
```

**Fix:**
```java
mockMvc.perform(post(URL).contentType(...).content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("SUB-005"));
```
```

```markdown
### Fix for EC-02 in CreateSubscriptionApiIT.java (missing test)

**Problem:** `SUB-006` (contract price ≤ 0) has no integration test.

**Fix:** Add the following test inside the `DomainRuleViolations` nested class:

```java
@Test
@DisplayName("Should return 400 SUB-006 when totalPrice is zero")
void shouldReturn400WhenTotalPriceIsZero() throws Exception {
    // Arrange
    CreateSubscriptionCommand command = new CreateSubscriptionCommand(
            activePatientId, UUID.randomUUID(),
            LocalDate.now().plusDays(1), LocalDate.now().plusDays(15),
            "LUNCH", BigDecimal.ZERO, "Acepto términos.", "Av. X", "1", "Lima",
            null, -12.0, -77.0, "+51999", LocalTime.of(12,0), LocalTime.of(14,0), null);
    String body = objectMapper.writeValueAsString(command);

    // Act & Assert
    mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("SUB-006"));
}
```
```
