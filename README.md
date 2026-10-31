<!-- [![Review Assignment Due Date](https://classroom.github.com/assets/deadline-readme-button-22041afd0340ce965d47ae6ef1cefeee28c7c493a6346c4f15d667ab976d596c.svg)](https://classroom.github.com/a/4gfZ4JAR) -->

# Diseño Táctico DDD — BC3: Suscripción y Calendario de Catering

**Estudiante:** Alvaro Molina

## Descripción del Microservicio

**Propósito:**
El propósito de este microservicio (BC3) es gestionar de extremo a extremo las **Suscripciones y el Calendario de Catering** de los pacientes. Actúa como el motor central del negocio, controlando las reglas de cuándo, dónde y cómo deben entregarse los planes nutricionales adquiridos, asegurando que se respeten los términos del contrato de servicio.

**Funcionalidades Principales:**

1. **Gestión de Suscripciones:** Creación, cancelación y finalización de contratos (de 15 o 30 días), validando que un paciente no tenga contratos duplicados activos.
2. **Control de Calendario de Entregas:** Generación de un calendario personal por suscripción, donde el paciente puede modificar la dirección, el horario y las instrucciones de una entrega específica con al menos 48 horas de anticipación.
3. **Pausas y Reactivaciones:** Capacidad de suspender temporalmente el servicio (con 48 horas de aviso) y reactivarlo, desplazando automáticamente la fecha de fin del contrato original.
4. **Programación de Evaluaciones:** Cálculo y agendamiento automático de citas de control corporal según el plan (evitando fines de semana y días no laborables).
5. **Consolidación Diaria para Operaciones:** Cierre diario que cruza todas las suscripciones activas del sistema para generar el **Calendario Consolidado**, un artefacto inmutable que contiene las "líneas de entrega" del día y sirve como la orden oficial que detona la producción en Cocina (BC4) y el despacho en Logística (BC5).

## Diagrama de Clases de la Capa de Dominio

A continuación se presentan los diagramas de clases del modelo de dominio. Dichos diagramas son **exclusivos de este microservicio (BC3)** y reflejan la implementación de la arquitectura limpia en la capa de dominio.

# **1\. Identificación del Modelo de Dominio**

## **1.1 Aggregates y sus Aggregate Roots**

| Aggregate                  | Aggregate Root        | Descripción                                                                                                                                                                       |
| :------------------------- | :-------------------- | :-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Suscripción**            | Suscripcion           | Contrato activo de catering de un paciente. Gestiona el calendario de entregas, pausas y evaluaciones durante el período contratado (15 o 30 días).                               |
| **Calendario Consolidado** | CalendarioConsolidado | Agrupación diaria e inmutable de todas las entregas activas del sistema para una fecha específica. Es el artefacto que activa la producción en cocina (BC4) y la logística (BC5). |

##

## **1.2 Entidades**

| Entidad              | Aggregate             | Descripción                                                                                                                                                                  |
| :------------------- | :-------------------- | :--------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| CalendarioDeEntregas | Suscripción           | Programación de los días de entrega para el período vigente de una suscripción.                                                                                              |
| DiaDeEntrega         | Suscripción           | Representa una entrega física en una fecha concreta. Tiene dirección, ventana horaria e instrucciones propias que el paciente puede modificar con hasta 48h de anticipación. |
| SolicitudDePausa     | Suscripción           | Solicitud formal de suspensión temporal del servicio. Registra el rango de fechas pausadas y permite la reactivación anticipada.                                             |
| EvaluacionQuincenal  | Suscripción           | Cita de control corporal incluida automáticamente al crear la suscripción. Se genera 1 evaluación para planes de 15 días y 2 para planes de 30 días.                         |
| LineaConsolidado     | CalendarioConsolidado | Representa una entrega individual dentro del calendario diario de producción y logística. Contiene todos los datos necesarios para que BC4 y BC5 operen de forma autónoma.   |

##

## **1.3 Value Objects**

| Value Object          | Tipo         | Descripción                                                                                                                                                         |
| :-------------------- | :----------- | :------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| ContratoDeServicio    | VO Compuesto | Registra los términos acordados al momento de la firma: plan contratado, período, tipo de servicio, precio y condiciones aceptadas.                                 |
| PreferenciasDeEntrega | VO Compuesto | Configuración por defecto del paciente para sus entregas: dirección principal, ventana horaria e instrucciones especiales. Se usa como base para cada DíaDeEntrega. |
| DireccionDeEntrega    | VO Compuesto | Dirección física completa de entrega: calle, número, ciudad, referencia y coordenadas GPS (latitud/longitud) para geolocalización logística.                        |
| VentanaHoraria        | VO Compuesto | Franja horaria de entrega definida por horaInicio y horaFin (ej: 12:00–13:00). Reutilizable en DíaDeEntrega, PreferenciasDeEntrega y LineaConsolidado.              |
| PeriodoDeVigencia     | VO Compuesto | Rango de fechas \[fechaInicio, fechaFin\] del contrato. Calcula automáticamente la fecha de fin a partir de la duración.                                            |
| RangoDePausa          | VO Compuesto | Período de inicio y fin de una pausa solicitada. Valida que fechaInicio sea al menos 48 horas en el futuro.                                                         |
| CodigoDeContrato      | VO Primitivo | Identificador único legible del contrato. Formato: NTC-YYYY-NNNN. Generado automáticamente al crear la suscripción.                                                 |
| DuracionPlan          | Enum         | Duración predefinida del plan de suscripción (15 o 30 días).                                                                                                        |
| TipoDeServicio        | Enum         | Modalidad de comidas del contrato.                                                                                                                                  |
| EstadoSuscripcion     | Enum         | Estado actual del ciclo de vida de la suscripción.                                                                                                                  |
| EstadoDiaEntrega      | Enum         | Estado de una entrega individual en el calendario (PROGRAMADO, PAUSADO, CONSOLIDADO, ENTREGADO, NO_ENTREGA, FALLIDO, CANCELADO).                                    |
| EstadoEvaluacion      | Enum         | Estado de la cita de evaluación quincenal.                                                                                                                          |
| EstadoConsolidado     | Enum         | Estado del ciclo de vida del CalendarioConsolidado.                                                                                                                 |

##

## **1.4 Read Model**

| Read Model         | Origen                                                  | Descripción                                                                                                                                                          |
| :----------------- | :------------------------------------------------------ | :------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| PacienteReferencia | Eventos de BC1 (PacienteRegistrado, PacienteInactivado) | Réplica local del estado de activación de los pacientes. Contiene pacienteId, activo (boolean) y actualizadoAt. Se mantiene actualizada mediante eventos del broker. |

##

## **1.5 Domain Services**

| Domain Service                         | Descripción                                                                                                    |
| :------------------------------------- | :------------------------------------------------------------------------------------------------------------- |
| **GeneradorDeEvaluacionesQuincenales** | Calcula las fechas exactas de las evaluaciones considerando días hábiles y feriados.                           |
| **ValidadorDeDuplicidadDeSuscripción** | Consulta repositorios para asegurar que un paciente no tenga más de una suscripción activa o pausada (INV-01). |
| **ConsolidadorDiario**                 | Orquestador que cruza todas las suscripciones activas para armar el CalendarioConsolidado diario.              |

##

## **1.6 Diagrama de Clases — Estructura de Aggregates**

![][image1]

##

## **1.7 Diagrama de Clases — Catálogo de Value Objects**

![][image2]

## **1.8 Diagrama de Clases — Modelo Completo**

![][image3]

[image1]: https://public-resources-mc.s3.us-east-1.amazonaws.com/v2/Diagrama+Clases+-+Agregates-v2.png
[image2]: https://public-resources-mc.s3.us-east-1.amazonaws.com/v2/CatalogoValueObjects-v2.png
[image3]: https://public-resources-mc.s3.us-east-1.amazonaws.com/v2/ms-catering-subscription-v2.png

---

## 2\. Ejecución con Docker

### 2.1 Imagen publicada en Docker Hub

| Tag      | Estado      | URL                                                      | Descripción |
| :------- | :---------- | :------------------------------------------------------- | :---------- |
| `1.1.0`  | **Nueva**   | https://hub.docker.com/r/alv641/ms-catering-subscription | Netflix Eureka Client, Kafka Consumers/Producers e IDs compactos (8 chars). |
| `latest` | **Actual**  | https://hub.docker.com/r/alv641/ms-catering-subscription | Apunta a la última versión estable (`1.1.0`). |
| `1.0.2`  | Anterior    | https://hub.docker.com/r/alv641/ms-catering-subscription | Versión inicial monolítica con base de datos MySQL. |

Para descargar la última versión:

```bash
docker pull alv641/ms-catering-subscription:latest
```

---

### 2.2 Variables de Entorno del Contenedor

El contenedor de `ms-catering-subscription` se configura mediante las siguientes variables de entorno:

#### Base de Datos (MySQL)
| Variable | Descripción | Valor por defecto |
| :--- | :--- | :--- |
| `SPRING_DATASOURCE_URL` | URL JDBC de conexión a MySQL | `jdbc:mysql://127.0.0.1:3325/ms_catering_subscription?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC` |
| `SPRING_DATASOURCE_USERNAME` | Usuario de la base de datos | `admin_db` |
| `SPRING_DATASOURCE_PASSWORD` | Contraseña de la base de datos | `123456` |

#### Service Discovery (Netflix Eureka)
| Variable | Descripción | Valor por defecto |
| :--- | :--- | :--- |
| `EUREKA_SERVER_URL` | URL del endpoint `defaultZone` de Eureka Server | `http://localhost:8761/eureka/` |
| `EUREKA_REGISTER_WITH_EUREKA` | Habilitar auto-registro en Eureka (`true`/`false`) | `true` |
| `EUREKA_FETCH_REGISTRY` | Habilitar descarga del registro de servicios (`true`/`false`) | `true` |
| `EUREKA_PREFER_IP_ADDRESS` | Registrar la IP del contenedor en Eureka en vez de su hostname | `true` |

#### Mensajería de Eventos (Apache Kafka)
| Variable | Descripción | Valor por defecto |
| :--- | :--- | :--- |
| `KAFKA_BOOTSTRAP_SERVERS` | Dirección del broker de Kafka | `localhost:9092` |
| `KAFKA_PATIENT_TOPIC` | Tópico de eventos de pacientes (BC1) | `bc1.pacientes.eventos` |
| `KAFKA_PLAN_TOPIC` | Tópico de eventos de planes nutricionales (BC2) | `bc2.planes.eventos` |
| `KAFKA_DELIVERY_TOPIC` | Tópico de eventos de entregas y logística (BC5) | `bc5.logistica.eventos` |
| `KAFKA_PATIENT_GROUP_ID` | Consumer group para eventos de pacientes | `ms-catering-bc1-patient-consumer` |

#### Servidor HTTP y Actuator
| Variable | Descripción | Valor por defecto |
| :--- | :--- | :--- |
| `SERVER_PORT` | Puerto HTTP interno en el contenedor | `8080` |

---

### 2.3 Formas de Ejecución

#### Opción A: Contenedor Independiente (`docker run`)
Ideal para integrar el microservicio conectándolo a un Eureka Server, Kafka y MySQL existentes (en el host o en otra red):

```bash
docker run -d \
  --name ms-catering-subscription \
  -p 8080:8080 \
  -e EUREKA_SERVER_URL=http://<eureka-host>:8761/eureka/ \
  -e EUREKA_PREFER_IP_ADDRESS=true \
  -e SPRING_DATASOURCE_URL="jdbc:mysql://<db-host>:3306/ms_catering_subscription?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC" \
  -e SPRING_DATASOURCE_USERNAME=admin_db \
  -e SPRING_DATASOURCE_PASSWORD=123456 \
  -e KAFKA_BOOTSTRAP_SERVERS=<kafka-host>:9092 \
  alv641/ms-catering-subscription:latest
```

#### Opción B: Entorno Local con Docker Compose

1. Copiar el archivo de variables de entorno y ajustar si es necesario:
   ```bash
   cp .env.example .env
   ```

2. Levantar todos los servicios en segundo plano:
   ```bash
   docker compose up -d
   ```

3. Ver los logs de la aplicación en tiempo real:
   ```bash
   docker compose logs -f app
   ```

---

### 2.4 Endpoints de Verificación y Documentación

Una vez levantado el contenedor:

- **Health check (Actuator):** `GET http://localhost:8080/actuator/health`
- **Contrato OpenAPI 3.0 (JSON):** `GET http://localhost:8080/api-docs`
- **Rutas de Negocio Principales:**
  - `POST /api/subscriptions` (Crear suscripción)
  - `GET /api/subscriptions/{id}` (Consultar suscripción por ID)
  - `POST /api/subscriptions/{id}/pause` (Pausar suscripción)
  - `POST /api/subscriptions/{id}/cancel` (Cancelar suscripción)
  - `GET /api/consolidated-calendars?date=YYYY-MM-DD` (Consultar calendario consolidado diario)
  - `POST /api/consolidated-calendars/close` (Ejecutar cierre y consolidación de catering)

---

### 2.5 Detener el entorno Docker Compose

```bash
# Detener servicios (conserva los datos de la BD)
docker compose down

# Detener y eliminar todos los datos (volumen MySQL)
docker compose down -v
```

---

## 3. Estrategia Integral de Testing

El microservicio cuenta con una suite completa de pruebas estructurada bajo la pirámide de testing y los lineamientos de Clean Architecture + DDD:

```
                  ▲
                 / \
                /   \     Contract Tests (Pact JVM)
               /-----\    Consumer & Provider Verification
              /       \
             /---------\  Integration Tests (*IT.java)
            /           \ Full Vertical Slice (Controller → Pipelinr → H2)
           /-------------\
          /               \ Unit Tests (*Test.java)
         /-----------------\ Pure Java (Domain, Application, Mappers)
```

---

### 3.1 Stack de Testing

| Herramienta | Versión | Rol / Propósito |
| :--- | :--- | :--- |
| **JUnit 5 (Jupiter)** | 5.x | Motor de ejecución de pruebas |
| **AssertJ** | 3.x | Aserciones fluidas y legibles |
| **Mockito** | 5.x | Creación de dobles de prueba y verificación de interacciones |
| **JaCoCo** | 0.8.12 | Generación de métricas y reportes visuales de cobertura de código |
| **Spring Boot Test + MockMvc** | 4.x | Arnés de integración con contexto completo y base de datos H2 en memoria |
| **Pact JVM Consumer & Provider** | 4.6.15 | Consumer-Driven Contract Testing (CDCT) para APIs REST |

---

### 3.2 Pruebas Unitarias (Unit Tests)

Las pruebas unitarias validan la lógica de negocio y aplicación en estricto aislamiento, sin levantar contextos pesados ni bases de datos.

- **`domain`**: Entidades, Agregados (`Suscripcion`, `CalendarioConsolidado`), Value Objects (`PeriodoDeVigencia`, `PreferenciasDeEntrega`, `DireccionDeEntrega`, etc.) y Domain Services puros.
- **`application`**: Command Handlers, Query Handlers y validadores de negocio orquestados mediante Pipelinr, utilizando mocks para los puertos de persistencia y eventos.
- **`infrastructure`**: Mappers de persistencia (`SubscriptionMapperTest`, `ConsolidatedCalendarMapperTest`, `PatientReferenceMapperTest`).

#### Comandos de ejecución

```bash
# Ejecutar todas las pruebas unitarias de todos los módulos
mvn test

# Ejecutar pruebas unitarias de un módulo específico
mvn -pl domain test
mvn -pl application test
mvn -pl infrastructure test -Dtest="*MapperTest"
```

---

### 3.3 Cobertura de Código (Code Coverage con JaCoCo)

El proyecto tiene integrado el plugin `jacoco-maven-plugin` (0.8.12) configurado para recolectar datos de ejecución y generar reportes HTML automáticamente durante la fase de prueba.

#### Umbral de cumplimiento
> **Requisito mínimo exigido:** 80% de cobertura.
>
> **Métricas actuales obtenidas en el proyecto:**
> - **Módulo `domain`:** **95%** de cobertura
> - **Módulo `application`:** **90%** de cobertura
> - **Módulo `infrastructure`:** **87%** de cobertura

#### Dónde encontrar los reportes generados
Al ejecutar `mvn test` o `mvn verify`, se generan los reportes navegables en:

| Módulo | Ruta del Reporte HTML |
| :--- | :--- |
| **Domain** | `domain/target/site/jacoco/index.html` |
| **Application** | `application/target/site/jacoco/index.html` |
| **Infrastructure** | `infrastructure/target/site/jacoco/index.html` |

#### Cómo visualizar los reportes

**Opción 1: Abrir desde la terminal (Linux / macOS)**
```bash
# Abrir el reporte del dominio
xdg-open domain/target/site/jacoco/index.html

# Abrir el reporte de aplicación
xdg-open application/target/site/jacoco/index.html

# Abrir el reporte de infraestructura
xdg-open infrastructure/target/site/jacoco/index.html
```

**Opción 2: Abrir con un navegador web específico**
```bash
google-chrome domain/target/site/jacoco/index.html
# o
firefox application/target/site/jacoco/index.html
```

**Opción 3: Explorador de archivos / IDE**
Hacer clic derecho sobre cualquier archivo `index.html` dentro de la carpeta `target/site/jacoco/` del módulo deseado y seleccionar **Open with Browser** (o *Show in System Explorer*).

---

### 3.4 Pruebas de Integración (Integration Tests)

Las pruebas de integración (`*IT.java`) prueban el **slice vertical completo** de la aplicación:
`MockMvc HTTP Request` ➔ `Controller` ➔ `Pipelinr Pipeline` ➔ `TransactionalMiddleware` ➔ `Handler` ➔ `Repository / JPA` ➔ `Base de Datos H2 en memoria (con migraciones reales de Liquibase)`.

#### Flujos de Negocio Cubiertos

Las pruebas están agrupadas bajo `infrastructure/src/test/java/.../api/` y cubren dos flujos completos de la aplicación:

1. **Flujo 1: Ciclo de Vida Completo de la Suscripción y Preferencias**
   - **Creación**: Registro de una nueva suscripción validando duplicidad (`CreateSubscriptionApiIT`).
   - **Consulta**: Obtención del detalle completo de la suscripción y sus días de entrega (`GetSubscriptionDetailsApiIT`).
   - **Modificación**: Cambio de preferencias de entrega (`UpdateDeliveryPreferencesApiIT`) y ajuste específico de un día de entrega (`ModifyDeliveryDayApiIT`).
   - **Pausa y Reactivación**: Solicitud de pausa temporal con recálculo de vigencia (`PauseSubscriptionApiIT`) y reactivación anticipada (`ReactivateSubscriptionApiIT`).
   - **Cierre**: Cancelación formal (`CancelSubscriptionApiIT`) y finalización de contrato (`CompleteSubscriptionApiIT`).

2. **Flujo 2: Operaciones Diarias de Catering, Evaluaciones y Sincronización**
   - **Gestión de Entregas**: Confirmación de entrega exitosa (`ConfirmDeliveryApiIT`), registro de entrega fallida por ausencia (`RegisterFailedDeliveryApiIT`) y marcado de día sin entrega programada (`MarkNoDeliveryApiIT`).
   - **Evaluaciones Quincenales**: Registro y completitud de controles corporales del paciente (`MarkEvaluationCompletedApiIT`).
   - **Sincronización Read Model**: Sincronización de pacientes desde eventos de BC1 (`PatientApiIT`).

#### Comandos de ejecución

```bash
# Ejecutar únicamente las pruebas de integración (*IT.java aisladas de unit y pact)
mvn -pl infrastructure test-compile failsafe:integration-test

# Ejecutar una prueba de integración específica
mvn -pl infrastructure verify -Dit.test=CreateSubscriptionApiIT
```

---

### 3.5 Pruebas de Contrato con Pact (Contract Testing)

El proyecto implementa **Consumer-Driven Contract Testing (CDCT)** mediante **Pact JVM 4.6.15**. Permite garantizar que el microservicio proveedor cumple con los contratos acordados con sus consumidores sin requerir entornos distribuidos activos.

```
[Consumer Test] ➔ Pact Mock Server ➔ Genera Contrato JSON en /pacts
                                                    ↓
[Provider Test] ➔ Verifica Contrato ➔ Spring Boot Real (RANDOM_PORT + H2)
```

- **Consumidor (Consumer):** `NutricenterPortalConsumer` (Portal Web / App cliente de BC3).
- **Proveedor (Provider):** `ms-catering-subscription` (API REST de BC3).
- **Archivo de Contrato Generado:** [`pacts/NutricenterPortalConsumer-ms-catering-subscription.json`](pacts/NutricenterPortalConsumer-ms-catering-subscription.json).

#### Flujos acordados en el Contrato
1. `POST /api/v1/subscriptions`: Solicitud para crear una nueva suscripción de 15 días (espera `201 Created` y payload validado).
2. `GET /api/v1/subscriptions/{id}`: Solicitud para consultar detalles de una suscripción existente (espera `200 OK` con contrato validado).

#### Orden y Comandos de Ejecución

> **Importante:** El test del consumidor debe ejecutarse **antes** que el test del proveedor para asegurar que el contrato JSON esté generado en `pacts/`.

```bash
# Paso 1: Ejecutar el test del Consumidor para generar el contrato JSON
mvn -pl infrastructure test -Dtest=SubscriptionConsumerPactTest

# Paso 2: Ejecutar el test del Proveedor para verificar el contrato contra Spring Boot
mvn -pl infrastructure test -Dtest=SubscriptionProviderPactTest

# Ejecutar ambos secuencialmente en un solo comando
mvn -pl infrastructure test -Dtest="SubscriptionConsumerPactTest,SubscriptionProviderPactTest"
```

---

### 3.6 Ejecución Completa de Toda la Suite

Para compilar, ejecutar todos los unit tests, contract tests, integration tests y verificar cobertura en todo el proyecto:

```bash
mvn clean verify
```

---

### 3.7 Reglas de Proyecto y Skills de Agentes (Trazabilidad)

En cumplimiento de las buenas prácticas de ingeniería y la transparencia en el uso de IA y asistentes de código, se incluyen todas las directrices, arneses y skills utilizados:

#### Reglas de Proyecto (`project/`)
- [`project/unit-testing-rules.md`](project/unit-testing-rules.md): Reglas de arquitectura, aislamiento, nomenclatura y aserciones para Unit Testing.
- [`project/integration-test-rules.md`](project/integration-test-rules.md): Estándares del arnés de integración con Spring Boot, MockMvc y Pipelinr.
- [`project/contract-testing-rules.md`](project/contract-testing-rules.md): Especificaciones de Pact JVM, definición de estados (`@State`) y reglas de CDCT.

#### Skills de Agentes (`.agents/skills/`)
- [`.agents/skills/unit-test-writer/`](.agents/skills/unit-test-writer/): Skill para generación de pruebas unitarias por capa.
- [`.agents/skills/unit-test-verifier/`](.agents/skills/unit-test-verifier/): Skill auditor de conformidad de pruebas unitarias.
- [`.agents/skills/integration-test-writer/`](.agents/skills/integration-test-writer/): Skill para generación de pruebas de integración de endpoints.
- [`.agents/skills/integration-test-verifier/`](.agents/skills/integration-test-verifier/): Skill auditor de reglas de pruebas de integración.
- [`.agents/skills/pact-writer/`](.agents/skills/pact-writer/): Skill para creación de pruebas de consumidor y proveedor Pact.
- [`.agents/skills/pact-verifier/`](.agents/skills/pact-verifier/): Skill auditor de contratos Pact.
