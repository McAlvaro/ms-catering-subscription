# Contratos De Integracion De Salida BC3 v1

> Estado: **congelado por OUT-001**. Fuente funcional: eventos 10 a 14 de
> `/home/alvaro/Downloads/TareaActividad2-tabla-corregida.md`. Este documento y
> sus [esquemas JSON Schema](schemas/v1/) son la autoridad para la version `1.0`.

## Alcance y convenciones

BC3 publica cinco eventos de integracion. Los seis eventos de dominio salientes
se traducen a estos cinco contratos; `SubscriptionCancelled` y
`SubscriptionCompleted` comparten el contrato de baja.

- Los valores publicos de `eventName` son exactamente los que aparecen abajo.
- Los identificadores de negocio en `data` son UUID serializados como `string`.
  Esta es una excepcion aprobada a la convencion de identificadores con prefijo
  de la tabla canonica. No se publican codigos `SUS-`, `PAC-`, `PLAN-`, etc.
- `eventId` es un UUID tecnico del hecho publicado. `correlationId` no puede ser
  vacio y se propaga sin cambios durante un mismo flujo de negocio.
- `occurredOn` usa ISO-8601 en UTC y termina en `Z`; `source` siempre es
  `MsCateringSubscription`; `eventVersion` siempre es `1.0`.
- `moneda` siempre es `BOB`.
- Los destinos son nombres logicos: no presuponen exchange, topic, cola ni
  proveedor de broker.

## Envelope comun

Todos los eventos cumplen
[`integration-event-envelope.schema.json`](schemas/v1/integration-event-envelope.schema.json).
Todos sus campos son obligatorios.

| Campo | Tipo | Regla | Origen |
| --- | --- | --- | --- |
| `eventId` | UUID string | Obligatorio; unico por hecho | `DomainEvent.eventId()` |
| `eventName` | string | Nombre publico fijo del contrato | Traductor segun el tipo de dominio |
| `eventVersion` | string | Constante `1.0` | Contrato congelado |
| `occurredOn` | ISO-8601 UTC string | Obligatorio; sufijo `Z` | `DomainEvent.occurredAt()` |
| `source` | string | Constante `MsCateringSubscription` | Contrato congelado |
| `correlationId` | string no vacio | Se propaga sin cambios | Contexto de comando/evento entrante; OUT-002 debe capturarlo |
| `data` | object | Payload especifico, sin campos adicionales | Snapshot del evento de dominio |

## Eventos, destinos y consumidores

| `eventName` | Esquema | Destino logico | Consumidores |
| --- | --- | --- | --- |
| `bc3.SuscripcionCreada` | `suscripcion-creada.schema.json` | `bc3.suscripcion.creada.v1` | MsPatientsConsulCore |
| `bc3.SuscripcionPausada` | `suscripcion-pausada.schema.json` | `bc3.suscripcion.pausada.v1` | MsPatientsConsulCore, MsNotificaciones |
| `bc3.SuscripcionReactivada` | `suscripcion-reactivada.schema.json` | `bc3.suscripcion.reactivada.v1` | MsPatientsConsulCore, MsNotificaciones |
| `bc3.SuscripcionCancelada` | `suscripcion-cancelada.schema.json` | `bc3.suscripcion.cancelada.v1` | MsPatientsConsulCore, MSNutritionPlanner, MsNotificaciones |
| `bc3.CalendarioConsolidadoCerrado` | `calendario-consolidado-cerrado.schema.json` | `bc3.calendario-consolidado.cerrado.v1` | MsProductionPackaging |

BC4 y BC5 consumen el calendario cerrado como snapshot operativo; no consumen
los eventos del ciclo de vida de la suscripcion.

## Payloads exactos

### `bc3.SuscripcionCreada`

| Campo de `data` | Tipo | Obligatorio | Origen |
| --- | --- | --- | --- |
| `idSuscripcion` | UUID string | Si | Snapshot de suscripcion |
| `idPaciente` | UUID string | Si | Snapshot de paciente |
| `idPlan` | UUID string | Si | Snapshot del plan |
| `tipoPlan` | string: `BASE` o `PERSONALIZADO` | Si | Snapshot del plan |
| `duracionDias` | integer >= 1 | Si | Contrato de servicio |
| `fechaInicio`, `fechaFin` | date | Si | Vigencia del contrato |
| `evaluacionesIncluidas` | integer >= 0 | Si | Contrato de servicio |
| `montoFacturado` | number >= 0 | Si | Facturacion al alta |
| `moneda` | string `BOB` | Si | Contrato congelado |
| `nroFactura` | string no vacio | Si | Facturacion al alta |

### `bc3.SuscripcionPausada`

| Campo de `data` | Tipo | Obligatorio | Origen |
| --- | --- | --- | --- |
| `idSuscripcion` | UUID string | Si | Snapshot de suscripcion |
| `idPaciente` | UUID string | Si | Snapshot de paciente |
| `fechaDesde`, `fechaHasta` | date | Si | Rango de pausa |
| `motivo` | string no vacio | Si | Solicitud de pausa |
| `nuevaFechaFin` | date | Si | Vigencia recalculada |
| `fechaSolicitud` | date | Si | Solicitud de pausa |

### `bc3.SuscripcionReactivada`

| Campo de `data` | Tipo | Obligatorio | Origen |
| --- | --- | --- | --- |
| `idSuscripcion` | UUID string | Si | Snapshot de suscripcion |
| `idPaciente` | UUID string | Si | Snapshot de paciente |
| `fechaReactivacion` | date | Si | Reactivacion efectiva |
| `fechaFin` | date | Si | Vigencia vigente tras la pausa |

### `bc3.SuscripcionCancelada`

| Campo de `data` | Tipo | Obligatorio | Origen |
| --- | --- | --- | --- |
| `idSuscripcion` | UUID string | Si | Snapshot de suscripcion |
| `idPaciente` | UUID string | Si | Snapshot de paciente |
| `idPlan` | UUID string | Si | Snapshot del plan |
| `tipoBaja` | `MANUAL` o `VENCIMIENTO` | Si | Tipo de evento de dominio |
| `motivo` | string no vacio | Si | Cancelacion o motivo fijo de vencimiento |
| `fechaEfectiva` | date | Si | Fecha efectiva de la baja |
| `entregasPendientesAnuladas` | integer >= 0 | Si | Snapshot de calendario |

### `bc3.CalendarioConsolidadoCerrado`

| Campo de `data` | Tipo | Obligatorio | Origen |
| --- | --- | --- | --- |
| `idCalendario` | UUID string | Si | Calendario consolidado |
| `fechaEntrega` | date | Si | Calendario consolidado |
| `fechaCierre` | ISO-8601 date-time | Si | Cierre del calendario |
| `totalEntregas` | integer >= 1 | Si | Cantidad de `entregas` al cerrar |
| `entregas` | array no vacio | Si | Lineas consolidadas del snapshot |
| `entregas[].idCalendarioDia` | UUID string | Si | Linea consolidada |
| `entregas[].idSuscripcion` | UUID string | Si | Linea consolidada |
| `entregas[].idPaciente` | UUID string | Si | Linea consolidada |
| `entregas[].nombrePaciente` | string no vacio | Si | Proyeccion de paciente |
| `entregas[].idPlan` | UUID string | Si | Linea consolidada |
| `entregas[].numDiaPlan` | integer >= 1 | Si | Plan aplicado |
| `entregas[].direccion` | object completo | Si | Direccion resuelta de la linea |
| `entregas[].direccion.idDireccion` | UUID string | Si | Direccion resuelta |
| `entregas[].direccion.alias` | string no vacio | Si | Direccion resuelta |
| `entregas[].direccion.direccion` | string no vacio | Si | Direccion resuelta |
| `entregas[].direccion.referencia` | string no vacio | Si | Direccion resuelta |
| `entregas[].direccion.ciudad` | string no vacio | Si | Direccion resuelta |
| `entregas[].direccion.latitud` | number [-90, 90] | Si | Direccion resuelta |
| `entregas[].direccion.longitud` | number [-180, 180] | Si | Direccion resuelta |
| `entregas[].recetas` | array no vacio | Si | Plan aplicado |
| `entregas[].recetas[].idReceta` | UUID string | Si | Receta del plan |
| `entregas[].recetas[].nombre` | string no vacio | Si | Receta del plan |
| `entregas[].recetas[].hora` | string `HH:mm` | Si | Tiempo de comida |
| `entregas[].recetas[].porcionGramos` | number > 0 | Si | Receta del plan |

## Matriz DomainEvent -> IntegrationEvent

| DomainEvent | IntegrationEvent | Regla de traduccion | Datos disponibles hoy | Brecha para OUT-002 |
| --- | --- | --- | --- | --- |
| `SubscriptionCreated` | `bc3.SuscripcionCreada` | Traduccion uno a uno | `eventId`, `occurredAt`, suscripcion, paciente | Plan, vigencia, evaluaciones, importe, factura, correlacion y snapshot completo |
| `SubscriptionPaused` | `bc3.SuscripcionPausada` | Traduccion uno a uno | `eventId`, `occurredAt`, suscripcion, rango | Paciente, motivo, nueva fecha fin, fecha solicitud y correlacion; corregir extension de vigencia |
| `SubscriptionReactivated` | `bc3.SuscripcionReactivada` | Traduccion uno a uno | `eventId`, `occurredAt`, suscripcion, fecha | Paciente, fecha fin resultante y correlacion |
| `SubscriptionCancelled` | `bc3.SuscripcionCancelada` | `tipoBaja=MANUAL` | `eventId`, `occurredAt`, suscripcion, motivo | Paciente, plan, fecha efectiva, entregas anuladas y correlacion |
| `SubscriptionCompleted` | `bc3.SuscripcionCancelada` | `tipoBaja=VENCIMIENTO`; motivo fijo `Finalizacion del periodo contratado` | `eventId`, `occurredAt`, suscripcion | Paciente, plan, fecha efectiva, entregas anuladas y correlacion |
| `ConsolidatedCalendarClosed` | `bc3.CalendarioConsolidadoCerrado` | Traduccion uno a uno | `eventId`, `occurredAt`, calendario, fecha, total | Fecha cierre, correlacion y lineas completas con paciente, direccion y recetas |

## Compatibilidad v1

1. `eventName`, `eventVersion`, `source` y nombres/tipos de campos v1 no se
   cambian ni se reutilizan con otro significado.
2. Un cambio incompatible crea una nueva version y un nuevo esquema/destino
   logico `vN`; v1 continua disponible durante la migracion acordada.
3. Una adicion opcional futura requiere una revision de contrato y no puede
   convertir un campo v1 obligatorio en opcional ni viceversa.
4. Los consumidores deben tolerar reentregas y deduplicar por `eventId` cuando
   se implemente el consumo. El orden global entre eventos no esta garantizado.
5. Los schemas v1 rechazan campos no documentados dentro de `data`; un productor
   no debe enviar extensiones silenciosas.

## Brechas conocidas para OUT-002

- Capturar snapshots inmutables completos en los seis `DomainEvent` antes de
  persistir el outbox; el relay no puede consultar el estado actual.
- Propagar y persistir el `correlationId` del comando o evento entrante.
- Resolver las proyecciones de paciente, plan, recetas, direccion, facturacion
  y numero de factura necesarias para los payloads.
- Corregir pausa/reactivacion para calcular `nuevaFechaFin`, `fechaFin` y las
  entregas sustitutas reales.
- Convertir todos los IDs internos requeridos a UUID string en el snapshot.
- Construir el calendario cerrado con lineas, direcciones completas y recetas;
  el evento de dominio actual solo contiene id, fecha y total.

## Ejemplos canonicos v1

### `bc3.SuscripcionCreada`

```json
{
  "eventId": "3f2a0010-9c1e-4b7a-a1d2-7e5f0c8b1010",
  "eventName": "bc3.SuscripcionCreada",
  "eventVersion": "1.0",
  "occurredOn": "2026-10-20T16:00:00Z",
  "source": "MsCateringSubscription",
  "correlationId": "corr-alta-8a78228a-1df9-4bc8-bc22-12f5f7c7b901",
  "data": {
    "idSuscripcion": "7f1e2d3c-4b5a-4c6d-8e9f-001122334455",
    "idPaciente": "8a78228a-1df9-4bc8-bc22-12f5f7c7b901",
    "idPlan": "c2d4e6f8-1357-4a9b-8cde-2468ace02468",
    "tipoPlan": "PERSONALIZADO",
    "duracionDias": 30,
    "fechaInicio": "2026-10-22",
    "fechaFin": "2026-11-20",
    "evaluacionesIncluidas": 2,
    "montoFacturado": 2850.00,
    "moneda": "BOB",
    "nroFactura": "F-2026-004512"
  }
}
```

### `bc3.SuscripcionPausada`

```json
{
  "eventId": "3f2a0011-9c1e-4b7a-a1d2-7e5f0c8b1011",
  "eventName": "bc3.SuscripcionPausada",
  "eventVersion": "1.0",
  "occurredOn": "2026-10-25T14:00:00Z",
  "source": "MsCateringSubscription",
  "correlationId": "corr-pausa-7f1e2d3c-4b5a-4c6d-8e9f-001122334455-01",
  "data": {
    "idSuscripcion": "7f1e2d3c-4b5a-4c6d-8e9f-001122334455",
    "idPaciente": "8a78228a-1df9-4bc8-bc22-12f5f7c7b901",
    "fechaDesde": "2026-10-28",
    "fechaHasta": "2026-10-29",
    "motivo": "VIAJE",
    "nuevaFechaFin": "2026-11-22",
    "fechaSolicitud": "2026-10-25"
  }
}
```

### `bc3.SuscripcionReactivada`

```json
{
  "eventId": "3f2a0012-9c1e-4b7a-a1d2-7e5f0c8b1012",
  "eventName": "bc3.SuscripcionReactivada",
  "eventVersion": "1.0",
  "occurredOn": "2026-10-30T04:00:00Z",
  "source": "MsCateringSubscription",
  "correlationId": "corr-pausa-7f1e2d3c-4b5a-4c6d-8e9f-001122334455-01",
  "data": {
    "idSuscripcion": "7f1e2d3c-4b5a-4c6d-8e9f-001122334455",
    "idPaciente": "8a78228a-1df9-4bc8-bc22-12f5f7c7b901",
    "fechaReactivacion": "2026-10-30",
    "fechaFin": "2026-11-22"
  }
}
```

### `bc3.SuscripcionCancelada`

```json
{
  "eventId": "3f2a0013-9c1e-4b7a-a1d2-7e5f0c8b1013",
  "eventName": "bc3.SuscripcionCancelada",
  "eventVersion": "1.0",
  "occurredOn": "2026-11-23T03:59:59Z",
  "source": "MsCateringSubscription",
  "correlationId": "corr-baja-7f1e2d3c-4b5a-4c6d-8e9f-001122334455",
  "data": {
    "idSuscripcion": "7f1e2d3c-4b5a-4c6d-8e9f-001122334455",
    "idPaciente": "8a78228a-1df9-4bc8-bc22-12f5f7c7b901",
    "idPlan": "c2d4e6f8-1357-4a9b-8cde-2468ace02468",
    "tipoBaja": "VENCIMIENTO",
    "motivo": "Finalizacion del periodo contratado",
    "fechaEfectiva": "2026-11-22",
    "entregasPendientesAnuladas": 0
  }
}
```

### `bc3.CalendarioConsolidadoCerrado`

```json
{
  "eventId": "3f2a0014-9c1e-4b7a-a1d2-7e5f0c8b1014",
  "eventName": "bc3.CalendarioConsolidadoCerrado",
  "eventVersion": "1.0",
  "occurredOn": "2026-10-22T03:59:59Z",
  "source": "MsCateringSubscription",
  "correlationId": "corr-entrega-2026-10-23",
  "data": {
    "idCalendario": "6b9f4d2e-54f1-4b8d-a8d7-9b6f2c3d4e50",
    "fechaEntrega": "2026-10-23",
    "fechaCierre": "2026-10-21T23:59:59-04:00",
    "totalEntregas": 2,
    "entregas": [
      {
        "idCalendarioDia": "5e7d8c9b-0a1f-4b2c-8d3e-4f5a6b7c8d90",
        "idSuscripcion": "7f1e2d3c-4b5a-4c6d-8e9f-001122334455",
        "idPaciente": "8a78228a-1df9-4bc8-bc22-12f5f7c7b901",
        "nombrePaciente": "Juan Perez Rojas",
        "idPlan": "c2d4e6f8-1357-4a9b-8cde-2468ace02468",
        "numDiaPlan": 2,
        "direccion": {
          "idDireccion": "d4e5f6a7-b8c9-4d0e-8f1a-2b3c4d5e6f70",
          "alias": "Trabajo",
          "direccion": "Av. Cristo Redentor 1450, Torre Norte, piso 3",
          "referencia": "Frente al Banco Union",
          "ciudad": "Santa Cruz de la Sierra",
          "latitud": -17.7612,
          "longitud": -63.1825
        },
        "recetas": [
          { "idReceta": "11111111-2222-4333-8444-555555555555", "nombre": "Avena cocida con frutos rojos", "hora": "08:00", "porcionGramos": 250 },
          { "idReceta": "22222222-3333-4444-8555-666666666666", "nombre": "Yogur natural con semillas de girasol", "hora": "08:00", "porcionGramos": 150 },
          { "idReceta": "33333333-4444-4555-8666-777777777777", "nombre": "Sopa de verduras", "hora": "13:00", "porcionGramos": 300 },
          { "idReceta": "44444444-5555-4666-8777-888888888888", "nombre": "Pechuga a la plancha con quinua y vegetales", "hora": "19:30", "porcionGramos": 350 }
        ]
      },
      {
        "idCalendarioDia": "4d3c2b1a-0f9e-4876-8d5c-4b3a29181716",
        "idSuscripcion": "9c8b7a6d-5e4f-4321-8a9b-0c1d2e3f4a5b",
        "idPaciente": "1a2b3c4d-5e6f-4789-8abc-def012345678",
        "nombrePaciente": "Maria Lopez Vaca",
        "idPlan": "0f1e2d3c-4b5a-4678-8c9d-0e1f2a3b4c5d",
        "numDiaPlan": 2,
        "direccion": {
          "idDireccion": "e5f6a7b8-c9d0-4e1f-8a2b-3c4d5e6f7081",
          "alias": "Domicilio",
          "direccion": "Calle Junin 245, Barrio Centro",
          "referencia": "Porton verde",
          "ciudad": "Santa Cruz de la Sierra",
          "latitud": -17.7840,
          "longitud": -63.1810
        },
        "recetas": [
          { "idReceta": "11111111-2222-4333-8444-555555555555", "nombre": "Avena cocida con frutos rojos", "hora": "08:00", "porcionGramos": 250 },
          { "idReceta": "33333333-4444-4555-8666-777777777777", "nombre": "Sopa de verduras", "hora": "13:00", "porcionGramos": 300 },
          { "idReceta": "55555555-6666-4777-8888-999999999999", "nombre": "Ensalada fresca", "hora": "13:00", "porcionGramos": 200 },
          { "idReceta": "44444444-5555-4666-8777-888888888888", "nombre": "Pechuga a la plancha con quinua y vegetales", "hora": "19:30", "porcionGramos": 350 }
        ]
      }
    ]
  }
}
```

## Limites de JSON Schema

Draft 2020-12 exige que `totalEntregas` y `entregas` sean positivos/no vacios,
pero no puede expresar de forma estandar la igualdad dinamica
`totalEntregas == entregas.length`. Esa igualdad se valida en el traductor y en
las pruebas de OUT-006; los ejemplos canonicos la cumplen.
