package com.mcalvaro.mscatering.integration.contracts;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record SubscriptionReactivatedIntegrationEvent(UUID eventId, Instant occurredOn, String correlationId, Data data)
        implements IntegrationEvent {
    public static final String EVENT_NAME = "bc3.SuscripcionReactivada";
    public static final String LOGICAL_DESTINATION = "bc3.suscripcion.reactivada.v1";

    public SubscriptionReactivatedIntegrationEvent {
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
            @JsonProperty("fechaReactivacion") LocalDate reactivatedOn,
            @JsonProperty("fechaFin") LocalDate endDate) {
        public Data {
            ContractAssertions.required(subscriptionId, "subscriptionId");
            ContractAssertions.required(patientId, "patientId");
            ContractAssertions.date(reactivatedOn, "reactivatedOn");
            ContractAssertions.date(endDate, "endDate");
        }
    }
}
