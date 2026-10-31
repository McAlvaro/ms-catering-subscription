package com.mcalvaro.mscatering.integration.contracts;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record SubscriptionCreatedIntegrationEvent(UUID eventId, Instant occurredOn, String correlationId, Data data)
        implements IntegrationEvent {
    public static final String EVENT_NAME = "bc3.SuscripcionCreada";
    public static final String LOGICAL_DESTINATION = "bc3.suscripcion.creada.v1";

    public SubscriptionCreatedIntegrationEvent {
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
            @JsonProperty("idSuscripcion") String subscriptionId,
            @JsonProperty("idPaciente") String patientId,
            @JsonProperty("idPlan") String planId,
            @JsonProperty("tipoPlan") String planType,
            @JsonProperty("duracionDias") int durationDays,
            @JsonProperty("fechaInicio") LocalDate startDate,
            @JsonProperty("fechaFin") LocalDate endDate,
            @JsonProperty("evaluacionesIncluidas") int includedEvaluations,
            @JsonProperty("montoFacturado") BigDecimal invoicedAmount,
            @JsonProperty("nroFactura") @JsonInclude(JsonInclude.Include.ALWAYS) String invoiceNumber) {
        public Data {
            ContractAssertions.text(subscriptionId, "subscriptionId");
            ContractAssertions.text(patientId, "patientId");
            ContractAssertions.text(planId, "planId");
            if (!"BASE".equals(planType) && !"PERSONALIZADO".equals(planType)) {
                throw new IllegalArgumentException("planType must be BASE or PERSONALIZADO");
            }
            ContractAssertions.positive(durationDays, "durationDays");
            ContractAssertions.date(startDate, "startDate");
            ContractAssertions.date(endDate, "endDate");
            ContractAssertions.nonNegative(includedEvaluations, "includedEvaluations");
            ContractAssertions.nonNegative(invoicedAmount, "invoicedAmount");
            if (invoiceNumber != null) {
                throw new IllegalArgumentException("invoiceNumber must be null for BC3");
            }
        }

        @JsonProperty("moneda")
        public String currency() {
            return "BOB";
        }
    }
}
