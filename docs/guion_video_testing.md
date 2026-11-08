# Guion para el Video de Presentación de Testing
**Microservicio:** BC3 — Suscripción y Calendario de Catering (`ms-catering-subscription`)  
**Proyecto:** Nur-tricenter — Caso de Estudio Trabajo Final  
**Estudiante:** Alvaro Molina  
**Duración estimada:** 4 a 5 minutos (Conciso y al grano)

---

## 📋 Preparación Previa (Antes de Grabar)

Ten listo en tu escritorio:
1. **IDE (VS Code / IntelliJ):**
   - Árbol de directorios abierto mostrando: `domain/src/test`, `application/src/test`, `infrastructure/src/test`, `project/` y `.agents/skills/`.
2. **Terminal (fuente legible, limpia):**
   - Comandos preparados en el historial de bash (flecha arriba para no escribir lento).
3. **Navegador Web con pestañas ya abiertas:**
   - Pestaña 1: `domain/target/site/jacoco/index.html`
   - Pestaña 2: `application/target/site/jacoco/index.html`
   - Pestaña 3: `infrastructure/target/site/jacoco/index.html`
   - Pestaña 4: Archivo de contrato `pacts/NutricenterPortalConsumer-ms-catering-subscription.json` (o en el IDE).

---

## 🎬 ESTRUCTURA DEL GUION

```
[00:00 - 00:30] 1. Introducción y Alcance
[00:30 - 01:15] 2. Arquitectura de Testing y Trazabilidad (Skills & Reglas)
[01:15 - 02:15] 3. Pruebas Unitarias y Reporte de Code Coverage (JaCoCo ≥ 80%)
[02:15 - 03:30] 4. Pruebas de Integración (2 Flujos Completos en Spring Boot)
[03:30 - 04:30] 5. Pruebas de Contrato con Pact (Consumer-Driven Contract Testing)
[04:30 - 04:50] 6. Conclusión y Cierre
```

---

### Bloque 1: Introducción y Alcance [00:00 - 00:30]

* **En pantalla:** Vista general del proyecto en el IDE y archivo `README.md` abierto en la sección 3.
* **Qué decir:**
> *"Buenas noches docente / compañeros. Mi nombre es Alvaro Molina. En este video presento la implementación de la capa completa de testing para el microservicio **ms-catering-subscription**, correspondiente al Bounded Context 3 (Suscripción y Calendario de Catering) del sistema Nur-tricenter.*
> 
> *A lo largo de la presentación mostraré la suite de pruebas unitarias con su reporte de cobertura JaCoCo superando el 80% requerido, la verificación de los dos flujos completos mediante pruebas de integración sobre Spring Boot, la implementación de Contract Testing con Pact y las reglas y skills que respaldan la trazabilidad del código generado."*

---

### Bloque 2: Arquitectura y Trazabilidad (Skills & Reglas) [00:30 - 01:15]

* **En pantalla:** Desplegar en el explorador del IDE las carpetas:
  - `project/` (`unit-testing-rules.md`, `integration-test-rules.md`, `contract-testing-rules.md`).
  - `.agents/skills/` (`unit-test-writer`, `integration-test-writer`, `pact-writer`, etc.).
* **Qué decir:**
> *"Siguiendo la arquitectura limpia y DDD, el microservicio está dividido en tres módulos Maven: **domain**, **application** e **infrastructure**.*
> 
> *Para cumplir con el requerimiento de la tarea sobre el uso de generación asistida, el repositorio cuenta con trazabilidad total:*
> * En la carpeta **`project/`** definimos los arneses y directrices técnicas de diseño de pruebas: reglas unitarias, de integración y de contratos Pact.
> * En **`.agents/skills/`** incluimos las herramientas especializadas de escritura y verificación de pruebas que aseguran que el código de testing sea estricto, repetible y cumpla con la arquitectura hexagonal sin acoplamientos indebidos."*

---

### Bloque 3: Pruebas Unitarias y Reporte de Cobertura JaCoCo [01:15 - 02:15]

* **En pantalla:** 
  1. Terminal ejecutando `mvn -pl domain,application test`.
  2. Navegador web mostrando los reportes HTML de JaCoCo.
* **Comando en Terminal (Solo pruebas unitarias puras):**
  ```bash
  mvn -pl domain,application test
  ```
* **Qué decir:**
> *"Comenzamos con la base de la pirámide: las pruebas unitarias. Ejecutamos `mvn -pl domain,application test` para correr las pruebas de las capas de negocio en estricto aislamiento.*
> 
> *(Mientras termina en ~6 segundos o mostrando el BUILD SUCCESS):*
> * *En **domain** testeamos en puro Java los Agregados de Suscripción y Calendario Consolidado, y los Value Objects como PeriodoDeVigencia y PreferenciasDeEntrega.*
> * *En **application** testeamos todos los Command y Query Handlers orquestados con Pipelinr, aislando los puertos de persistencia con Mockito.*
> 
> *Pasando al requerimiento de cobertura de código (mínimo 80%), abrimos los reportes generados por JaCoCo:*
> * *Módulo **Domain**: alcanzamos un **95%** de cobertura de instrucciones.*
> * *Módulo **Application**: alcanzamos un **90%** de cobertura.*
> * *Módulo **Infrastructure**: alcanzamos un **87%** de cobertura.*
> 
> *Como se observa, todos los módulos sobrepasan con holgura el umbral del 80% exigido por la actividad."*

---

### Bloque 4: Pruebas de Integración (2 Flujos Completos) [02:15 - 03:30]

* **En pantalla:** 
  1. Mostrar en el IDE la carpeta `infrastructure/src/test/java/.../api/subscription/`.
  2. Terminal ejecutando las pruebas de integración con Maven Failsafe.
* **Comando en Terminal (Solo ITs, sin unit tests ni pact tests):**
  ```bash
  mvn -pl infrastructure test-compile failsafe:integration-test
  ```
* **Qué decir:**
> *"Para la capa de integración, implementamos pruebas de extremo a extremo que prueban el slice vertical completo: desde la petición HTTP con MockMvc, pasando por el Pipeline y TransactionalMiddleware, hasta los repositorios y la base de datos H2 en memoria con migraciones reales de Liquibase.*
> 
> *Hemos agrupado los tests para cubrir **dos flujos completos de negocio**:*
> 
> 1. * **Flujo 1: Ciclo de Vida Completo de la Suscripción:** Probamos la creación del contrato validando duplicidad de paciente (`CreateSubscriptionApiIT`), consulta de detalle (`GetSubscriptionDetailsApiIT`), actualización de preferencias y días específicos con la regla de 48 horas (`ModifyDeliveryDayApiIT`), pausas de servicio (`PauseSubscriptionApiIT`), reactivación anticipada y cancelación definitiva.*
> 2. * **Flujo 2: Operaciones Diarias de Catering y Sincronización:** Probamos la confirmación de entrega en domicilio (`ConfirmDeliveryApiIT`), registro de entregas fallidas (`RegisterFailedDeliveryApiIT`), no entrega por feriado o viaje (`MarkNoDeliveryApiIT`), completitud de evaluaciones quincenales y la sincronización del Read Model de pacientes desde el BC1 (`PatientApiIT`).*
> 
> *En la terminal vemos que se ejecutan los 69 tests de integración de Failsafe arrojando cero errores y cero fallas en BUILD SUCCESS."*

---

### Bloque 5: Pruebas de Contrato con Pact (Consumer-Driven Contract Testing) [03:30 - 04:30]

* **En pantalla:** 
  1. IDE mostrando `SubscriptionConsumerPactTest.java`, el archivo JSON generado en `pacts/` y `SubscriptionProviderPactTest.java`.
  2. Terminal ejecutando primero el Consumer y luego el Provider.
* **Comandos en Terminal:**
  ```bash
  # 1. Ejecutar el test del Consumidor para generar el contrato
  mvn -pl infrastructure test -Dtest=SubscriptionConsumerPactTest

  # 2. Ejecutar la verificación del Proveedor contra Spring Boot real
  mvn -pl infrastructure test -Dtest=SubscriptionProviderPactTest
  ```
* **Qué decir:**
> *"Como valor agregado en la integración entre microservicios y clientes, implementamos pruebas de contrato con **Pact JVM 4.6.15** siguiendo el enfoque Consumer-Driven Contract Testing.*
> 
> *Aquí modelamos a **NutricenterPortalConsumer** (el portal cliente) como consumidor y a **ms-catering-subscription** como proveedor:*
> 
> 1. *Primero ejecutamos `SubscriptionConsumerPactTest`. El consumidor define formalmente qué espera de los endpoints de creación y consulta de suscripciones. Esta prueba levanta un Mock Server de Pact y genera el contrato inmutable en la carpeta **`pacts/`**.*
> 2. *Luego ejecutamos `SubscriptionProviderPactTest`. Esta prueba levanta el contexto real de Spring Boot con `@SpringBootTest` en un puerto aleatorio, configura los estados de prueba con `@State` y verifica automáticamente que nuestra API cumpla punto por punto con el contrato JSON acordado.*
> 
> *Como vemos en la consola, ambas interacciones son verificadas con éxito sin necesidad de levantar entornos distribuidos complejos."*

---

### Bloque 6: Conclusión y Cierre [04:30 - 04:50]

* **En pantalla:** Terminal con los resultados finales o `README.md` resumen.
* **Qué decir:**
> *"En conclusión, la suite de pruebas del microservicio ms-catering-subscription cubre íntegramente los requisitos solicitados:*
> * *Pruebas unitarias con más del 87% al 95% de cobertura JaCoCo.*
> * *Pruebas de integración sobre Spring Boot cubriendo dos flujos completos de negocio.*
> * *Pruebas de contrato con Pact JVM.*
> * *Y la inclusión completa de las reglas y skills utilizadas en el repositorio.*
> 
> *Con esto concluyo la presentación. Muchas gracias por su atención."*

---

## 💡 Consejos de Grabación
* **Herramienta de grabación sugerida:** OBS Studio o la grabadora de pantalla integrada de tu sistema operativo (e.g. SimpleScreenRecorder o atajo `Ctrl + Alt + Shift + R`).
* **Resolución:** 1080p a pantalla completa para que el código y la terminal se lean con nitidez.
* **Ritmo:** Habla pausado pero con convicción. Con los comandos ya preparados no tardarás más de 4 a 5 minutos.
