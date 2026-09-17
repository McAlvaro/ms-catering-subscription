# Agent Routing Rules

## Integration Test Generator

Whenever the user's message contains **any** of the following intents — in Spanish
or English, using any phrasing — delegate the entire request to the
**`integration-test-generator`** subagent without asking for confirmation:

### Trigger phrases (examples, not exhaustive)

| Spanish | English |
|---|---|
| "genera integration tests para X" | "generate integration tests for X" |
| "crea las pruebas de integración de X" | "create integration tests for X" |
| "hazme un IT test para X" | "write an IT test for X" |
| "agrega tests de integración a X" | "add integration tests to X" |
| "actualiza los integration tests de X" | "update the integration tests for X" |
| "corrige los IT tests de X" | "fix the integration tests for X" |
| "revisa los integration tests de X" | "review / verify the IT tests for X" |
| "verifica las pruebas de integración de X" | "audit the integration tests for X" |
| "hace falta un test de integración para X" | "we need an integration test for X" |

Where **X** can be a controller name, a file path, a directory, or an endpoint description.

### How to delegate

Pass the user's original message verbatim (plus any file context from the IDE)
to the `integration-test-generator` subagent. Do not answer the request yourself.

---

## Unit Test Generator

Whenever the user's message asks to generate, create, update, fix, review, or
verify **unit tests** (not integration tests), delegate to the
**`unit-test-generator`** subagent.

Trigger words: "unit test", "prueba unitaria", "test unitario", "genera tests para X"
(when X is a domain or application class), "crea tests para X", "verifica tests de X".

---

## Pact Generator

Whenever the user's message contains **any** of the following intents — in Spanish
or English — delegate to the **`pact-generator`** subagent without asking for confirmation:

### Trigger phrases (examples, not exhaustive)

| Spanish | English |
|---|---|
| "genera pruebas de contrato para X" | "generate contract tests for X" |
| "crea el contrato Pact para X" | "create the Pact contract for X" |
| "agrega una interacción Pact para X" | "add a Pact interaction for X" |
| "verifica el contrato Pact de X" | "verify the Pact contract for X" |
| "genera el consumer test Pact de X" | "generate the consumer Pact test for X" |
| "genera el provider test Pact de X" | "generate the provider Pact test for X" |
| "actualiza el contrato Pact de X" | "update the Pact contract for X" |
| "audita las pruebas de contrato de X" | "audit the contract tests for X" |
| "necesitamos contract testing para X" | "we need contract testing for X" |

Where **X** can be a controller name, a resource name, or an endpoint description.
