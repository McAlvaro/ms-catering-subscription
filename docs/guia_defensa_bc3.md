# Guía de Defensa: Microservicio de Suscripción y Calendario (BC3)

Esta guía está diseñada para que la leas y te apoyes en ella durante tu presentación. Está dividida exactamente en las dos partes que te solicitaron: la demostración (Demo) y la exposición del código (Arquitectura).

---

## PARTE 1: Demo del Funcionamiento (El "Qué" hace)

**Objetivo:** Demostrar que el microservicio resuelve el problema de negocio planteado, aplicando reglas estrictas y manteniendo la consistencia de los datos.

### Paso 1: Inicializar el contexto (Crear Paciente)
*   **Acción:** Usa el endpoint `POST /api/patients` para simular que recibimos un evento del BC1 indicando que un paciente se registró y está activo.
*   **Discurso:** *"Para empezar, nuestro sistema necesita saber qué pacientes están activos. Dado que esto pertenece al BC1, simulamos la recepción del evento del broker actualizando nuestro Read Model local de Pacientes."*

### Paso 2: Crear una Suscripción (La regla de negocio principal)
*   **Acción:** Usa `POST /api/subscriptions` enviando un plan de 15 o 30 días. Luego haz un GET para mostrar la suscripción creada.
*   **Discurso:** *"Creamos una suscripción. El sistema calcula automáticamente los días de entrega. Además, aplicando la Regla RN-11 y RN-12, el sistema genera automáticamente las citas de evaluación quincenal, saltándose los fines de semana."*

### Paso 3: Modificar un Día de Entrega (Regla de 48 horas)
*   **Acción:** Toma el UUID de un día de entrega e intenta modificar su dirección con `PUT /api/subscriptions/{id}/delivery-days/{dayId}`.
*   **Discurso:** *"El paciente puede cambiar dónde quiere recibir su comida. Sin embargo, el Dominio protege la operación: si la entrega es en menos de 48 horas, el sistema lanzará una excepción de negocio rechazando el cambio para no afectar a logística."*

### Paso 4: Pausar el Servicio
*   **Acción:** Usa `POST /api/subscriptions/{id}/pause`.
*   **Discurso:** *"El usuario puede pausar su servicio por viaje o enfermedad. Al hacer esto, los días de entrega afectados cambian su estado a PAUSADO y, al reactivarse, el periodo de vigencia del contrato se extenderá automáticamente para compensar los días no consumidos."*

### Paso 5: Generar el Calendario Consolidado (El output hacia otros BCs)
*   **Acción:** Ejecuta `POST /api/consolidated-calendar` usando una fecha válida donde haya entregas `SCHEDULED`.
*   **Discurso:** *"Finalmente, el corazón operativo: el Cierre Diario. Este endpoint busca todas las suscripciones activas, extrae las entregas de esa fecha y genera el Calendario Consolidado. Es un documento inmutable que, al cerrarse, dispara un Evento de Dominio que activará a la Cocina (BC4) y a Logística (BC5)."*

---

## PARTE 2: Exposición del Código (El "Cómo" está construido)

**Objetivo:** Demostrar tu dominio sobre DDD (Domain-Driven Design) y Arquitectura Limpia (Clean Architecture). Muestra tu IDE y navega por estos archivos específicos.

### 1. La Capa de Dominio (Domain Layer)
**Discurso:** *"La regla de oro aquí es que esta capa no tiene dependencias de Spring Boot, ni de bases de datos, ni de JSON. Es Java puro que representa las reglas del negocio."*

*   **Los Agregados (Aggregate Roots):** Muestra la clase `Subscription.java`.
    *   *Qué explicar:* Es el punto de entrada. Nadie puede modificar un `DeliveryDay` directamente, tienen que pasar por el método `modifyDeliveryDay()` de la Suscripción, garantizando que el Agregado controle sus invariantes.
*   **Entidades vs Value Objects:**
    *   *Qué explicar:* Muestra `DeliveryDay.java` como **Entidad** (tiene un ID único porque su estado cambia en el tiempo) y luego muestra `ServiceContract.java` (implementado como un `record` de Java) como **Value Object** (es inmutable, no tiene ID propio, si cambia el contrato, se crea uno nuevo).
*   **Servicios de Dominio:** Muestra `BiweeklyEvaluationGenerator.java`.
    *   *Qué explicar:* *"Usamos un Domain Service para calcular las fechas de las evaluaciones porque esa lógica requiere saber si un día es hábil o fin de semana, lo cual no es responsabilidad directa de la Suscripción."*
*   **Eventos de Dominio:** Muestra el método `addDomainEvent()` al final de alguna acción (ej. en `Subscription.java`). *"Cuando algo importante pasa, el Agregado registra un evento (ej: DeliveryDayModified) para que otros componentes reaccionen."*

### 2. La Capa de Aplicación (Use Cases / Command Handlers)
**Discurso:** *"Aquí orquestamos el flujo de trabajo aplicando CQRS (Commands & Queries) mediante la librería Pipelinr. Esta capa tampoco sabe si usamos SQL o Mongo."*

*   Muestra `CreateSubscriptionCommandHandler.java`.
    *   *Qué explicar:* *"El Handler recibe un Comando, usa el repositorio para buscar o validar datos, invoca los métodos del Dominio (como `Subscription.create()`), y finalmente guarda el resultado en el repositorio."*

### 3. La Capa de Infraestructura (Infrastructure Layer)
**Discurso:** *"Aquí es donde conectamos nuestro dominio puro con el mundo exterior: Spring Boot, la base de datos PostgreSQL, la web y los eventos."*

*   **Adaptadores Web (Controladores):** Muestra `SubscriptionController.java`.
    *   *Qué explicar:* Recibe peticiones HTTP, arma el objeto `Command` y lo envía al `Pipeline`. El controlador no tiene lógica de negocio.
*   **Persistencia (Repositorios y Mappers):** Muestra `JpaSubscriptionRepository.java` y `SubscriptionMapper.java`.
    *   *Qué explicar:* *"Usamos Spring Data JPA, pero para no contaminar nuestro Dominio con anotaciones `@Entity` o constructores vacíos requeridos por Hibernate, tenemos un modelo JPA separado y un `Mapper`. El mapper usa `Unsafe.allocateInstance` para reconstruir los objetos del dominio desde la BD sin pasar por las validaciones de los constructores (ya que la data de la BD ya es válida)."*
*   **Manejo de Transacciones:** Muestra `TransactionalMiddleware.java`.
    *   *Qué explicar:* *"En lugar de poner `@Transactional` en la capa de Aplicación (lo cual acoplaría el negocio a Spring), usamos un Middleware en Pipelinr que envuelve la ejecución de cada comando en una transacción de base de datos de manera invisible para el caso de uso."*
*   **Publicación de Eventos:** Muestra `DomainEventLogger.java`.
    *   *Qué explicar:* *"Aquí interceptamos los Eventos de Dominio (usando `Notification.Handler` de Pipelinr) y los procesamos. En producción aquí usaríamos un `RabbitTemplate` para enviarlos a RabbitMQ o Kafka hacia BC4 y BC5."*
