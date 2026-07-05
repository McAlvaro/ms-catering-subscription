[![Review Assignment Due Date](https://classroom.github.com/assets/deadline-readme-button-22041afd0340ce965d47ae6ef1cefeee28c7c493a6346c4f15d667ab976d596c.svg)](https://classroom.github.com/a/4gfZ4JAR)

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
