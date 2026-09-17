---
name: pact-writer
description: >-
  Genera pruebas de contrato Pact JVM (Consumer-Driven Contract Testing) para
  el microservicio ms-catering-subscription. Produce el cliente HTTP del
  consumidor, las pruebas del consumidor que generan el contrato JSON, y las
  pruebas del proveedor que lo verifican contra el servidor Spring Boot real.
  Sigue estrictamente project/contract-testing-rules.md.
  Compatible con Antigravity, OpenCode, Claude y Codex.
---

# pact-writer

## Propósito

Generar los archivos de pruebas de contrato (Consumer-Driven Contract Testing)
utilizando Pact JVM 4.6.15 con JUnit 5, siguiendo la fuente de verdad única:
`project/contract-testing-rules.md`.

Trabaja para:
- **Un recurso concreto**: e.g., `SubscriptionController` → genera
  `SubscriptionClient.java`, `SubscriptionConsumerPactTest.java`,
  `SubscriptionProviderPactTest.java`.
- **Una nueva interacción**: agrega un nuevo `@Pact` y `@State` a los archivos
  existentes sin romper los contratos ya generados.

---

## Paso 1 — Leer las Reglas (Fuente de Verdad)

Antes de escribir cualquier código, **leer el archivo de reglas directamente**:

```
project/contract-testing-rules.md
```

Extraer:
- **Sección 0**: Roles Consumer/Provider, flujo de ejecución.
- **Sección 1**: Herramientas y versiones (Pact 4.6.15).
- **Sección 2**: Estructura de archivos y convenciones de nombres.
- **Secciones 3-4**: Reglas RC-* (consumidor) y RP-* (proveedor).
- **Sección 5**: Reglas de ejecución Maven.
- **Sección 6**: Prohibiciones globales.
- **Sección 7**: Catálogo de contratos actuales (para decidir si agregar o crear).

> **No hardcodear reglas.** Siempre seguir las definiciones activas en
> `project/contract-testing-rules.md`.

---

## Paso 2 — Análisis de Eligibilidad

Antes de generar nada, analizar el controlador objetivo:

| Criterio | Elegible (✅) | No elegible (❌) |
|---|---|---|
| ¿Es un `@RestController`? | Sí | No — omitir |
| ¿Expone endpoints HTTP REST? | Sí | No — omitir |
| ¿Existe su `Command`/`Query` en `application/`? | Sí | No — omitir |
| ¿El endpoint tiene sentido para un consumidor externo? | Sí | No — evaluar |

---

## Paso 3 — Decidir el Modo de Operación

### Modo A: Crear nuevos archivos de contrato

Cuando no existe `*ConsumerPactTest.java` para el recurso objetivo.

1. Crear `<Recurso>Client.java` en el paquete `contract/consumer/client/`.
2. Crear `<Recurso>ConsumerPactTest.java` en `contract/consumer/`.
3. Crear `<Recurso>ProviderPactTest.java` en `contract/provider/`.

### Modo B: Agregar interacción a un contrato existente

Cuando ya existe `*ConsumerPactTest.java` y se solicita una nueva interacción.

1. Agregar nuevo método `@Pact` en `*ConsumerPactTest.java`.
2. Agregar nuevo `@Test` con `@PactTestFor(pactMethod = "...")`.
3. Agregar nuevo `@State` en `*ProviderPactTest.java`.

---

## Paso 4 — Generar el Cliente HTTP del Consumidor

El cliente modela cómo el consumidor real haría la petición HTTP:

```java
public class SubscriptionClient {
    private final String baseUrl;
    private final HttpClient httpClient;

    public SubscriptionClient(String baseUrl) { ... }

    // Un método por endpoint pactado
    public HttpResponse<String> createSubscription(String body) throws ... { ... }
    public HttpResponse<String> getSubscriptionDetails(String id) throws ... { ... }
}
```

Reglas:
- Usar `java.net.http.HttpClient` (Java 11+, sin dependencias externas).
- Un método por endpoint (no mezclar múltiples endpoints en un método).
- Construir la URL como `baseUrl + "/api/subscriptions"`.

---

## Paso 5 — Generar la Prueba del Consumidor

Template base del `@Pact`:

```java
@Pact(consumer = "NutricenterPortalConsumer", provider = "ms-catering-subscription")
RequestResponsePact createSubscriptionPact(PactDslWithProvider builder) {
    return builder
        .given("estado previo que describe el escenario")
        .uponReceiving("descripción de la solicitud en español")
            .method("POST")
            .path("/api/subscriptions")
            .headers(Map.of("Content-Type", "application/json"))
            .body(new PactDslJsonBody()
                .uuid("patientId", "550e8400-e29b-41d4-a716-446655440000")
                // ... campos del request
            )
        .willRespondWith()
            .status(201)
            .headers(Map.of("Content-Type", "application/json"))
            .body(new PactDslJsonBody().uuid("value"))
        .toPact();
}
```

Reglas importantes:
- **`given(...)`**: El nombre debe coincidir exactamente con el `@State` del proveedor.
- **Request body**: Usar UUIDs fijos para entidades de estado (patientId, subscriptionId).
- **Response body**: Usar **matchers** (`uuid()`, `stringType()`, `decimalType()`) para
  valores generados dinámicamente. Usar `stringValue()` solo para enumeraciones conocidas.
- **`uponReceiving`**: Describir la interacción en lenguaje natural (español).

---

## Paso 6 — Generar la Prueba del Proveedor

Template base:

```java
@Provider("ms-catering-subscription")
@PactFolder("../pacts")
@SpringBootTest(classes = MsCateringApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Transactional
class SubscriptionProviderPactTest {

    @LocalServerPort int port;

    @BeforeEach
    void setUp(PactVerificationContext context) {
        context.setTarget(new HttpTestTarget("localhost", port));
    }

    @TestTemplate
    @ExtendWith(PactVerificationSpringProvider.class)
    void verifyPact(PactVerificationContext context) {
        context.verifyInteraction();
    }

    @State("estado previo que describe el escenario")
    void prepararEstado() {
        // Insertar datos necesarios vía repositorios reales
    }
}
```

Reglas importantes:
- **NO** usar `@MockBean` ni `@SpyBean`.
- Cada `@State` inserta datos a través de los repositorios de dominio (`ISubscriptionRepository`,
  `IPatientReferenceRepository`), no directamente por JPA/SQL.
- Los UUIDs usados en `@State` deben coincidir con los del contrato del consumidor.

---

## Paso 7 — Checklist de Prohibiciones (antes de finalizar)

Verificar que el código generado NO viola:

- [ ] No hay `@MockBean` en el Provider Test.
- [ ] Cada `@Pact` tiene al menos un `@Test` correspondiente.
- [ ] El nombre del `given(...)` coincide exactamente con el `@State`.
- [ ] Se usan matchers Pact para valores dinámicos en el response body.
- [ ] El cliente HTTP no hardcodea la URL (usa `mockServer.getUrl()`).
- [ ] El `@PactFolder` apunta a `"../pacts"`.
- [ ] La salida del contrato se genera en `pacts/` en la raíz.

---

## Paso 8 — Instrucciones de Ejecución para el Usuario

Tras generar los archivos, proporcionar:

```bash
# 1. Generar el contrato (ejecutar Consumer Test)
mvn -pl infrastructure test -Dtest=SubscriptionConsumerPactTest

# 2. Verificar el contrato (ejecutar Provider Test)  
mvn -pl infrastructure test -Dtest=SubscriptionProviderPactTest
```

Verificar que en `pacts/` exista el archivo:
`NutricenterPortalConsumer-ms-catering-subscription.json`
