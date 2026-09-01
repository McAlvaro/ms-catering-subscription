---
name: unit-test-writer
description: >-
  Generates JUnit 5 unit tests for a given Java source file or all files in a
  directory, strictly following the project's unit-testing-rules.md harness.
  Compatible with Antigravity, OpenCode, Claude, and Codex.
---

# unit-test-writer

## Purpose

Generate JUnit 5 unit test files for this project following the single source of truth: `project/unit-testing-rules.md`.

Works for:
- A **single Java file**: e.g., `domain/.../ValidityPeriod.java`
- A **directory**: e.g., `domain/.../vo/` — generates tests for all Java classes found.

---

## Step 1 — Read the Rules (Source of Truth)

Before writing any test, **read the rules file directly**:

```
project/unit-testing-rules.md
```

Extract the testing requirements directly from the rules file:
- **Stack & Libraries**: Testing framework and assertions to use
- **File Structure & Placement**: Mirror package path in `src/test/java/`
- **Layer-Specific Rules**: Rules applicable to Domain, Application, or Infrastructure
- **Code Conventions**: Method naming format, `@DisplayName`, AAA structure, class visibility
- **Global Prohibitions**: Forbidden patterns to avoid

> **Do not hardcode or assume rules.** Always adhere to the active definitions in `project/unit-testing-rules.md`.

---

## Step 2 — Identify Target Layer & Scope

Read the target source file(s) and determine:
1. **Module / Layer**:
   - `domain` → Pure unit tests with real domain instances (no Spring, no mocks for domain objects)
   - `application` → Mock external dependencies using Mockito (`@Mock`, `@InjectMocks`)
   - `infrastructure` → Slice tests (`@WebMvcTest`, `@DataJpaTest` with H2) or pure unit tests for mappers
2. **Behaviors to Test**:
   - Factory/Constructor validations and invariants
   - State mutations and returned results
   - Error handling and expected `DomainException` codes
   - Collaborator interactions and invocations

---

## Step 3 — Write the Test File

1. Place the test file mirroring the source package under `src/test/java/`.
2. Follow all conventions and rules extracted from `project/unit-testing-rules.md`:
   - Package-private test class visibility
   - Method naming format specified in the rules
   - Meaningful English `@DisplayName` on every test method
   - Explicit `// Arrange`, `// Act`, `// Assert` structure
   - Use `assertThatThrownBy` for exceptions (no empty `try/catch`)
   - Include both happy-path and edge/failure-path scenarios

---

## Step 4 — Verify Prohibitions Checklist

Before finalizing, review the generated test against the **Global Prohibitions** in `project/unit-testing-rules.md` to ensure zero violations.
