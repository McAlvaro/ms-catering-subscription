# Contract Testing Rules — ms-catering-subscription

> Harness de pruebas de contrato (Consumer-Driven Contract Testing) para el microservicio
> BC3 — Suscripción y Calendario de Catering.
> Implementado con Pact JVM 4.6.15 + JUnit 5.
> Aplica exclusivamente al módulo `infrastructure/`.

---

## 0. ¿Qué es Consumer-Driven Contract Testing?

El **Consumer-Driven Contract Testing (CDCT)** es una técnica donde el **consumidor** define
y documenta las expectativas sobre la API del **proveedor** a través de un contrato formal.
El contrato se genera automáticamente durante la ejecución de la prueba del consumidor y
luego es verificado contra el proveedor real, sin necesitar que ambos servicios corran
simultáneamente.

### Roles en este proyecto

| Rol | Nombre | Descripción |
|---|---|---|
| **Consumer** | `NutricenterPortalConsumer` | Portal Web / App del paciente que gestiona suscripciones. Modela cualquier cliente aguas abajo de BC3. |
| **Provider** | `ms-catering-subscription` | BC3 — el microservicio que expone la API REST de suscripciones. |

### Flujo de ejecución

```
[Consumer Test] → Pact Mock Server → genera → [Contrato JSON]
                                                      ↓
[Provider Test] ← lee el contrato ← verifica contra → [Controlador Spring Boot real]
```

---

## 1. Herramientas y Versiones

| Herramienta | Versión | Propósito |
|---|---|---|
| **Pact JVM Consumer** | `au.com.dius.pact.consumer:junit5:4.6.15` | DSL para definir y generar contratos |
| **Pact JVM Provider** | `au.com.dius.pact.provider:junit5:4.6.15` | Verificación del contrato contra el proveedor |
| **Pact JVM Provider Spring** | `au.com.dius.pact.provider:junit5spring:4.6.15` | Integración con Spring Boot (`@SpringBootTest`) |
| **JUnit 5** | 5.x (via Spring Boot parent) | Motor de ejecución |
| **Spring Boot Test** | 4.x | Contexto completo con H2 en memoria |
| **H2** | In-memory, MySQL mode | Base de datos de prueba |
| **Liquibase** | Auto-config | Migraciones reales aplicadas al iniciar |

---

## 2. Estructura de Archivos

```
infrastructure/src/test/java/
└── com/mcalvaro/mscatering/infrastructure/
    └── contract/
        ├── consumer/
        │   ├── client/
        │   │   └── SubscriptionClient.java        ← Cliente HTTP del consumidor
        │   └── SubscriptionConsumerPactTest.java  ← Genera el contrato
        └── provider/
            └── SubscriptionProviderPactTest.java  ← Verifica el contrato

pacts/
└── NutricenterPortalConsumer-ms-catering-subscription.json  ← Contrato generado (versionado en Git)
```

### Convención de nombres

| Artefacto | Patrón |
|---|---|
| Test del consumidor | `<Recurso>ConsumerPactTest.java` |
| Test del proveedor | `<Recurso>ProviderPactTest.java` |
| Cliente HTTP | `<Recurso>Client.java` |
| Archivo de contrato | `<Consumer>-<Provider>.json` |

---

## 3. Reglas del Consumidor (RC-*)

### RC-01 — Anotaciones obligatorias

Toda clase de prueba del consumidor debe llevar:
```java
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "ms-catering-subscription")
```

### RC-02 — Definición de Pacts

Cada interacción se define en un método anotado con `@Pact`:
```java
@Pact(consumer = "NutricenterPortalConsumer", provider = "ms-catering-subscription")
RequestResponsePact miInteraccionPact(PactDslWithProvider builder) { ... }
```

### RC-03 — Estado previo (given)

Toda interacción DEBE declarar un estado previo (`given(...)`) que describa
el estado del proveedor necesario para que la interacción sea válida.
El nombre del estado debe coincidir exactamente con el `@State` del proveedor.

### RC-04 — Uso de matchers

- Para campos cuyo valor exacto no es conocido por el consumidor, usar matchers:
  `stringType()`, `uuid()`, `numberType()`, `decimalType()`, `date()`, `time()`.
- Para campos cuyo valor exacto SÍ es conocido y forma parte del contrato, usar:
  `stringValue()`, `integerMatching()`, etc.

### RC-05 — Carpeta de salida del contrato

El contrato JSON se genera en la raíz del repositorio en `pacts/`. La propiedad del sistema
`pact.rootDir` configurada en `infrastructure/pom.xml` controla la ubicación hacia `${project.basedir}/../pacts`.
Esto permite versionar el archivo de contrato en Git y compartirlo con otros equipos.

### RC-06 — Tests de verificación del mock

Cada `@Pact` debe tener al menos un `@Test` con `@PactTestFor(pactMethod = "...")`.
El test ejecuta el cliente contra el mock server y aserta la respuesta recibida.

---

## 4. Reglas del Proveedor (RP-*)

### RP-01 — Anotaciones obligatorias

```java
@Provider("ms-catering-subscription")
@PactFolder("../pacts")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Transactional
```

### RP-02 — Ciclo de vida del test

```java
@BeforeEach
void setUp(PactVerificationContext context) {
    context.setTarget(new HttpTestTarget("localhost", port));
}

@TestTemplate
@ExtendWith(PactVerificationSpringProvider.class)
void verifyPact(PactVerificationContext context) {
    context.verifyInteraction();
}
```

### RP-03 — States (@State)

Cada estado declarado en el contrato del consumidor DEBE tener un método
correspondiente en el proveedor anotado con `@State("nombre exacto del estado")`.

El método `@State` es responsable de preparar los datos necesarios en H2
para que el endpoint responda según lo esperado en el contrato.

### RP-04 — Sin mocks en el proveedor

El Provider Test es una prueba de integración real. **Está prohibido**
el uso de `@MockBean`, `@SpyBean` o cualquier simulación en el proveedor.
Todo debe ejecutarse con beans reales y base de datos real (H2).

### RP-05 — Transaccionalidad

El Provider Test extiende `@Transactional`. Los datos preparados en `@State`
son visibles para el handler real durante la verificación de la interacción.

---

## 5. Reglas de Ejecución Maven (RE-*)

### RE-01 — Consumer primero, Provider después

El contrato JSON debe existir **antes** de ejecutar el Provider Test.
El orden correcto es:
```bash
# 1. Genera el contrato
mvn -pl infrastructure test -Dtest=SubscriptionConsumerPactTest

# 2. Verifica el contrato
mvn -pl infrastructure test -Dtest=SubscriptionProviderPactTest
```

### RE-02 — Exclusión de los tests de contrato en Failsafe

Los tests de contrato NO terminan en `*IT.java` y no son ejecutados por Failsafe.
Son pruebas de tipo `*Test.java` ejecutadas por Surefire, separadas de los tests
de integración de la capa HTTP con MockMvc.

---

## 6. Prohibiciones Globales

- ❌ No modificar manualmente el archivo `.json` de contratos generado por Pact.
- ❌ No usar `@MockBean` en `SubscriptionProviderPactTest`.
- ❌ No definir `@State` sin el correspondiente `@Pact` en el consumidor.
- ❌ No omitir el estado previo `given(...)` en ninguna interacción del consumidor.
- ❌ No usar valores literales fijos en los body de respuesta del consumidor
  cuando el valor es generado dinámicamente por el proveedor (usar matchers).

---

## 7. Contratos Actuales

| ID | Consumidor | Proveedor | Interacción | Endpoint |
|---|---|---|---|---|
| PACT-001 | NutricenterPortalConsumer | ms-catering-subscription | Crear suscripción de catering | POST /api/subscriptions |
| PACT-002 | NutricenterPortalConsumer | ms-catering-subscription | Consultar detalles de suscripción | GET /api/subscriptions/{id} |
