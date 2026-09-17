---
name: pact-verifier
description: >-
  Audita y verifica el cumplimiento de los archivos de pruebas de contrato
  Pact existentes (*ConsumerPactTest.java, *ProviderPactTest.java, *Client.java)
  contra las reglas definidas en project/contract-testing-rules.md.
  Emite un reporte PASS/FAIL por regla y archivo auditado.
  Compatible con Antigravity, OpenCode, Claude y Codex.
---

# pact-verifier

## Propósito

Auditar los archivos de contract testing de Pact en este proyecto y reportar
si cada uno cumple con las reglas definidas en `project/contract-testing-rules.md`.

Trabaja para:
- **Un archivo concreto**: e.g., `SubscriptionConsumerPactTest.java`
- **Un directorio**: e.g., `infrastructure/src/test/java/.../contract/`
  (audita todos los archivos de contrato que encuentre)

---

## Paso 1 — Leer las Reglas

Leer `project/contract-testing-rules.md` y extraer las reglas RC-* y RP-*.

---

## Paso 2 — Clasificar Archivos

Para cada archivo en el target, determinar su tipo:

| Tipo | Patrón | Reglas aplicables |
|---|---|---|
| Cliente HTTP del consumidor | `*Client.java` | RC-06 (tiene métodos por endpoint) |
| Test del consumidor | `*ConsumerPactTest.java` | RC-01, RC-02, RC-03, RC-04, RC-05, RC-06 |
| Test del proveedor | `*ProviderPactTest.java` | RP-01, RP-02, RP-03, RP-04, RP-05 |

---

## Paso 3 — Ejecutar Verificaciones

### Para *ConsumerPactTest.java

| ID Regla | Pregunta de verificación |
|---|---|
| RC-01 | ¿Tiene `@ExtendWith(PactConsumerTestExt.class)` y `@PactTestFor(providerName = "ms-catering-subscription")`? |
| RC-02 | ¿Cada `@Pact` retorna `RequestResponsePact`? ¿Tiene `(consumer = "NutricenterPortalConsumer", provider = "ms-catering-subscription")`? |
| RC-03 | ¿Cada interacción tiene `given(...)` con nombre no vacío? |
| RC-04 | ¿Se usan matchers Pact para valores dinámicos en el response body? |
| RC-05 | ¿No hay modificación manual del JSON generado (no archivo hardcodeado)? |
| RC-06 | ¿Cada `@Pact` tiene al menos un `@Test` con `@PactTestFor(pactMethod = ...)`? |

### Para *ProviderPactTest.java

| ID Regla | Pregunta de verificación |
|---|---|
| RP-01 | ¿Tiene `@Provider`, `@PactFolder`, `@SpringBootTest(webEnvironment = RANDOM_PORT)`, `@ActiveProfiles("test")`, `@Transactional`? |
| RP-02 | ¿Tiene `@BeforeEach` que configura `HttpTestTarget`? ¿Tiene `@TestTemplate` con `@ExtendWith(PactVerificationSpringProvider.class)`? |
| RP-03 | ¿Cada `@State` declarado en el consumidor tiene un método correspondiente en el proveedor? |
| RP-04 | ¿No hay `@MockBean` ni `@SpyBean` en el proveedor? |
| RP-05 | ¿Los datos se insertan vía repositorios de dominio (no SQL directo)? |

---

## Paso 4 — Reporte de Auditoría

Emitir el reporte en este formato exacto:

```
Pact Compliance Audit Report
════════════════════════════

📄 SubscriptionConsumerPactTest.java
────────────────────────────────────
RC-01 │ PASS │ Anotaciones obligatorias presentes
RC-02 │ PASS │ @Pact correctamente anotado (consumer + provider)
RC-03 │ PASS │ given() declarado en todas las interacciones
RC-04 │ PASS │ Matchers usados en response body
RC-05 │ N/A  │ No aplica (el archivo no modifica el JSON)
RC-06 │ FAIL │ El @Pact "createSubscriptionPact" no tiene @Test correspondiente

📄 SubscriptionProviderPactTest.java
─────────────────────────────────────
RP-01 │ PASS │ Todas las anotaciones obligatorias presentes
RP-02 │ PASS │ setUp y verifyPact correctamente definidos
RP-03 │ FAIL │ Falta @State para "a subscription with ID X exists"
RP-04 │ PASS │ Sin @MockBean ni @SpyBean
RP-05 │ PASS │ Datos insertados vía ISubscriptionRepository

Veredicto General: ❌ FAIL (2 violaciones encontradas)
```

Si todo está en orden:
```
Veredicto General: ✅ PASS — todos los archivos cumplen con contract-testing-rules.md
```

---

## Paso 5 — Correcciones (si hay FAILs)

Por cada FAIL:
1. Describir la violación exacta.
2. Mostrar el fragmento de código que la causa.
3. Proporcionar el fragmento corregido.
4. Pedir confirmación antes de aplicar cambios.
