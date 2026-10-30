package com.mcalvaro.mscatering.infrastructure.integration.inbound.bc5.delivery;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

/**
 * Inbound event contract for BC5 EntregaConfirmada.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DeliveryConfirmedEvent(
        @JsonProperty("eventId") UUID eventId,
        @JsonProperty("eventName") String eventName,
        @JsonProperty("eventVersion") String eventVersion,
        @JsonProperty("occurredOn") Instant occurredOn,
        @JsonProperty("source") String source,
        @JsonProperty("correlationId") String correlationId,
        @JsonProperty("data") DeliveryConfirmedData data
) {
}
