# Informe de Entrega — Capa de Testing

**Diplomado en Microservicios y Arquitecturas Cloud**  
**Caso de Estudio:** Nur-tricenter — Proyecto Final  
**Microservicio:** BC3: Suscripción y Calendario de Catering (`ms-catering-subscription`)  
**Estudiante:** Alvaro Molina

---

## Enlaces de Entrega

- **Repositorio de GitHub:** [https://github.com/McAlvaro/ms-catering-subscription](https://github.com/McAlvaro/ms-catering-subscription)
- **Video de Presentación (Google Drive):** [https://drive.google.com/file/d/1r2IynWa5QL4v83gXWBvp9HmDI7mNaQ7x/view?usp=sharing](https://drive.google.com/file/d/1r2IynWa5QL4v83gXWBvp9HmDI7mNaQ7x/view?usp=sharing)

---

## 1. Resumen de la Implementación

En este microservicio se implementó una estrategia integral de pruebas que abarca los tres niveles de la pirámide de testing, respetando los principios de Clean Architecture y Domain-Driven Design (DDD):

1. **Pruebas Unitarias:** Validación aislada de entidades, agregados, value objects y casos de uso en Java puro (sin dependencias de frameworks).
2. **Pruebas de Integración:** Validación del slice vertical completo sobre Spring Boot, MockMvc y base de datos H2 en memoria con migraciones reales de Liquibase.
3. **Pruebas de Contrato con Pact:** Verificación Consumer-Driven (CDCT) entre el portal cliente (`NutricenterPortalConsumer`) y la API del microservicio.
4. **Trazabilidad y Reglas:** Inclusión de directrices de testing en `project/` y skills especializadas en `.agents/skills/` para respaldar la generación asistida de pruebas.

---

## 2. Pruebas Unitarias y Cobertura de Código (JaCoCo)

Para las pruebas unitarias se utilizó **JUnit 5**, **Mockito** y **AssertJ**. El reporte de cobertura fue generado mediante el plugin `jacoco-maven-plugin` (0.8.12), superando el umbral del **80% mínimo exigido**:

### Evidencia de Cobertura JaCoCo:

```
+-------------------------------------------------------------------------+
|                                                                         |
|            [ PEGAR AQUÍ IMAGEN DE LA COBERTURA DE JACOCO ]              |
|                                                                         |
+-------------------------------------------------------------------------+
```

---

## 3. Pruebas de Integración (Flujos de Negocio Completos)

Las pruebas de integración (`*IT.java`) se ejecutan con Maven Failsafe y cubren **dos flujos completos de negocio** :

- **Flujo 1 — Ciclo de Vida Completo de la Suscripción:**  
  Comprende la creación del contrato con validación de duplicidad (`CreateSubscriptionApiIT`), consulta de detalles (`GetSubscriptionDetailsApiIT`), actualización de preferencias por defecto (`UpdateDeliveryPreferencesApiIT`), modificación de días individuales con la regla de negocio de 48 horas (`ModifyDeliveryDayApiIT`), solicitud de pausas con desplazamiento de vigencia (`PauseSubscriptionApiIT`), reactivación anticipada (`ReactivateSubscriptionApiIT`) y cancelación formal (`CancelSubscriptionApiIT`).

- **Flujo 2 — Operaciones Diarias de Catering y Sincronización:**  
  Comprende la confirmación de entregas exitosas (`ConfirmDeliveryApiIT`), registro de entregas fallidas (`RegisterFailedDeliveryApiIT`), marcado de días sin entrega (`MarkNoDeliveryApiIT`), completitud de evaluaciones corporales quincenales (`MarkEvaluationCompletedApiIT`) y sincronización del Read Model de pacientes desde eventos del BC1 (`PatientApiIT`).

---

## 4. Pruebas de Contrato con Pact (Consumer-Driven Contract Testing)

Se implementó Contract Testing mediante **Pact JVM 4.6.15** para validar los endpoints REST del microservicio contra las expectativas del cliente:

- **Consumidor:** `NutricenterPortalConsumer` (genera el contrato inmutable en `pacts/NutricenterPortalConsumer-ms-catering-subscription.json`).
- **Proveedor:** `ms-catering-subscription` (inicia el servidor Spring Boot real en puerto dinámico y verifica los estados `@State`).
- **Endpoints pactados:** `POST /api/v1/subscriptions` (creación) y `GET /api/v1/subscriptions/{id}` (consulta de detalle).

---

## 5. Trazabilidad de Reglas y Skills de IA

En cumplimiento con los requisitos de transparencia en el uso de herramientas generativas:

- **Reglas de Proyecto (`project/`):** [`unit-testing-rules.md`](project/unit-testing-rules.md), [`integration-test-rules.md`](project/integration-test-rules.md) y [`contract-testing-rules.md`](project/contract-testing-rules.md).
- **Skills Versionadas (`.agents/skills/`):** Skills de generación y auditoría (`unit-test-writer`, `integration-test-writer`, `pact-writer`, etc.).

---
