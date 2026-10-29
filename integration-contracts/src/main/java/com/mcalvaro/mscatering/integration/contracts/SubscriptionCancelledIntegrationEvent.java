package com.mcalvaro.mscatering.integration.contracts;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record SubscriptionCancelledIntegrationEvent(UUID eventId, Instant occurredOn, String correlationId, Data data)
        implements IntegrationEvent {
    public static final String EVENT_NAME = "bc3.SuscripcionCancelada";
    public static final String LOGICAL_DESTINATION = "bc3.suscripcion.cancelada.v1";

    public SubscriptionCancelledIntegrationEvent {
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
            @JsonProperty("idPlan") UUID planId,
            @JsonProperty("tipoBaja") String terminationType,
            @JsonProperty("motivo") String reason,
            @JsonProperty("fechaEfectiva") LocalDate effectiveDate,
            @JsonProperty("entregasPendientesAnuladas") int cancelledPendingDeliveries) {
        public Data {
            ContractAssertions.required(subscriptionId, "subscriptionId");
            ContractAssertions.required(patientId, "patientId");
            ContractAssertions.required(planId, "planId");
            if (!"MANUAL".equals(terminationType) && !"VENCIMIENTO".equals(terminationType)) {
                throw new IllegalArgumentException("terminationType must be MANUAL or VENCIMIENTO");
            }
            ContractAssertions.text(reason, "reason");
            ContractAssertions.date(effectiveDate, "effectiveDate");
            ContractAssertions.nonNegative(cancelledPendingDeliveries, "cancelledPendingDeliveries");
        }
    }
}
