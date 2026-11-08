# Datos de Prueba para la Defensa (Copy & Paste)

Para que tu presentación sea fluida y no pierdas tiempo escribiendo JSONs en vivo, aquí tienes todos los _payloads_ preparados para cada paso de la demo.

**Recomendación:** Ten este documento abierto en una mitad de tu pantalla y Swagger en la otra.

---

### Paso 1: Inicializar el contexto (Crear Paciente)

**Endpoint:** `POST /api/patients`

Copia y pega este JSON para registrar al paciente en el Read Model de BC3.

```json
{
  "patientId": "123e4567-e89b-12d3-a456-426614174000",
  "active": true,
  "updatedAt": "2026-07-04T12:00:00Z"
}
```

---

### Paso 2: Crear una Suscripción (La regla de 15 días)

**Endpoint:** `POST /api/subscriptions`

Este JSON crea una suscripción de exactamente 15 días (del 5 de Julio al 19 de Julio).

```json
{
  "patientId": "123e4567-e89b-12d3-a456-426614174000",
  "dietPlanId": "999e4567-e89b-12d3-a456-426614174999",
  "startDate": "2026-07-05",
  "endDate": "2026-07-19",
  "serviceType": "FULL",
  "totalPrice": 450.0,
  "acceptedConditions": "Acepto los términos de servicio",
  "prefStreet": "Av. Principal",
  "prefNumber": "123",
  "prefCity": "Santa Cruz",
  "prefReference": "Edificio Azul, Depto 4B",
  "prefLatitude": -17.7833,
  "prefLongitude": -63.1821,
  "prefPhone": "+59177777777",
  "prefTimeStart": "11:30:00",
  "prefTimeEnd": "13:00:00",
  "prefSpecialInstructions": "Sin maní, por favor."
}
```

---

### Paso 3: Modificar un Día de Entrega

**Endpoint:** `PUT /api/subscriptions/{id}/delivery-days/{dayId}`

1. Haz un `GET /api/subscriptions/{id}` con el UUID que copiaste.
2. Copia el UUID de un día de entrega (`dayId`) que sea para el **15 de Julio** o después (para asegurarnos de que cumpla la regla de las 48 horas).
3. Usa este JSON para enviarle la comida a su oficina ese día en específico:

```json
{
  "street": "Calle Secundaria",
  "number": "456",
  "city": "Santa Cruz",
  "reference": "Oficina Centro, Piso 2",
  "latitude": -17.78,
  "longitude": -63.18,
  "phone": "+59177777777",
  "startTime": "12:00:00",
  "endTime": "12:30:00",
  "instructions": "Dejar en recepción"
}
```

---

### Paso 4: Pausar el Servicio

**Endpoint:** `POST /api/subscriptions/{id}/pause`

Enviaremos una pausa para la próxima semana, cumpliendo perfectamente la regla de 48 horas de anticipación.

```json
{
  "startDate": "2026-07-10",
  "endDate": "2026-07-12",
  "reason": "Viaje de trabajo"
}
```

_(Luego puedes hacer un GET para mostrar a los profesores cómo las fechas 10, 11 y 12 cambiaron a estado "PAUSED" y cómo el contrato se alargó)._

---

### Paso 5: Generar el Calendario Consolidado (Cierre Diario)

**Endpoint:** `POST /api/consolidated-calendar`

Aquí simularemos ser el administrador que cierra el calendario del día **6 de Julio** (que es un día que no pausamos y está activo).

```json
{
  "date": "2026-07-06",
  "closedBy": "Admin Cocina"
}
```

_(Al ejecutar esto, muéstrales la terminal de tu IDE donde aparecerá el logo de fuego `🔥 DomainEvent: ConsolidatedCalendarClosed` demostrando que Cocina y Logística acaban de ser notificados)._
