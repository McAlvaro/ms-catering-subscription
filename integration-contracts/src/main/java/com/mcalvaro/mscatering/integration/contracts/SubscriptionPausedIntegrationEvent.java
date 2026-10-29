package com.mcalvaro.mscatering.integration.contracts;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record SubscriptionPausedIntegrationEvent(UUID eventId, Instant occurredOn, String correlationId, Data data)
        implements IntegrationEvent {
    public static final String EVENT_NAME = "bc3.SuscripcionPausada";
    public static final String LOGICAL_DESTINATION = "bc3.suscripcion.pausada.v1";

    public SubscriptionPausedIntegrationEvent {
        ContractAssertions.envelope(eventId, occurredOn, correlationId, data);
    }

    @Override
    public String eventName() {
        return EVENT_NAME;
    }

    @Override
    public String logicalDestination() {
        return LOGICAL_DESTINATION;
    }

    public record Data(
            @JsonProperty("idSuscripcion") UUID subscriptionId,
            @JsonProperty("idPaciente") UUID patientId,
            @JsonProperty("fechaDesde") LocalDate startDate,
            @JsonProperty("fechaHasta") LocalDate endDate,
            @JsonProperty("motivo") String reason,
            @JsonProperty("nuevaFechaFin") LocalDate newEndDate,
            @JsonProperty("fechaSolicitud") LocalDate requestedOn) {
        public Data {
            ContractAssertions.required(subscriptionId, "subscriptionId");
            ContractAssertions.required(patientId, "patientId");
            ContractAssertions.date(startDate, "startDate");
            ContractAssertions.date(endDate, "endDate");
            if (startDate.isAfter(endDate))
                throw new IllegalArgumentException("startDate must not follow endDate");
            ContractAssertions.text(reason, "reason");
            ContractAssertions.date(newEndDate, "newEndDate");
            ContractAssertions.date(requestedOn, "requestedOn");
        }
    }
}
