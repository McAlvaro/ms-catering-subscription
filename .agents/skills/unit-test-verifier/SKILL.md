---
name: unit-test-verifier
description: >-
  Verifies existing JUnit 5 test files against the rules defined in
  project/unit-testing-rules.md. Reports a PASS/FAIL verdict per rule for
  a given test file or directory. Designed to run after unit-test-writer.
  Compatible with Antigravity, OpenCode, Claude, and Codex.
---

# unit-test-verifier

## Purpose

Audit one or more `*Test.java` files against the single source of truth: `project/unit-testing-rules.md`.
Produces a structured verdict report with **PASS** or **FAIL** for each rule defined in the rules file.

Works for:
- A **single test file**: e.g., `domain/src/test/java/.../ValidityPeriodTest.java`
- A **directory**: e.g., `domain/src/test/java/.../vo/` — audits all `*Test.java` files found.

---

## Step 1 — Read the Rules (Source of Truth)

Before inspecting any test file, **read the rules file directly**:

```
project/unit-testing-rules.md
```

Extract all active rules directly from the file:
- **Layer-specific rules**: Domain (`R-D*`), Application (`R-A*`), Infrastructure (`R-I*`)
- **Code conventions**: Method naming, `@DisplayName`, meaningful assertions, AAA structure, class visibility
- **Global prohibitions**: All items in the global prohibitions table

> **Do not assume or hardcode rules.** Always evaluate against the exact content read from `project/unit-testing-rules.md`.

---

## Step 2 — Locate and Read the Target Test File(s)

1. If the input is a single file, read its content.
2. If the input is a folder, locate all `*Test.java` files recursively and read each one.
3. Identify the target layer for each file based on its file path and package declaration:
   - `domain` module / package → apply Domain rules + Conventions + Prohibitions
   - `application` module / package → apply Application rules + Conventions + Prohibitions
   - `infrastructure` module / package → apply Infrastructure rules + Conventions + Prohibitions

---

## Step 3 — Audit Against `project/unit-testing-rules.md`

For each file, evaluate every relevant rule from `project/unit-testing-rules.md`:
- **PASS**: The test complies with the rule.
- **FAIL**: The test violates the rule (record line number, offending code, and violated rule ID).
- **N/A**: The rule belongs to a different layer and does not apply to this file.

---

## Step 4 — Generate the Verdict Report

Output a clear report per file using the following structure:

```markdown
## Audit: <relative path to test file>
Layer: <domain | application | infrastructure>

| Rule ID / Section | Rule Description | Status | Detail / Line |
|---|---|---|---|
| <Rule ID> | <Description from rules file> | PASS | |
| <Rule ID> | <Description from rules file> | FAIL | Line <N>: <explanation> |

Overall Verdict: PASS | FAIL (<N> violations found)
```

If auditing multiple files or a directory, include a final summary:

```markdown
## Summary

| File | Violations | Overall |
|---|---|---|
| <TestFile1.java> | 0 | PASS |
| <TestFile2.java> | 2 | FAIL |

Total Files: <N> | Passed: <N> | Failed: <N>
```

---

## Step 5 — Suggest Targeted Fixes

For each **FAIL**, output a minimal, actionable fix showing only the snippet to replace:

```markdown
### Fix for <Rule ID> in <File> (Line <N>)

**Problem:** <Brief explanation of the violation>

**Current:**
```java
<offending code snippet>
```

**Fix:**
```java
<compliant replacement code snippet>
```
```
