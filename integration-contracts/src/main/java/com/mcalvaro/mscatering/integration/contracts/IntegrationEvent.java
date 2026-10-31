package com.mcalvaro.mscatering.integration.contracts;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

/** Stable broker-facing envelope for BC3 integration events. */
public interface IntegrationEvent {
    String EVENT_VERSION = "1.0";
    String SOURCE = "MsCateringSubscription";

    @JsonProperty("eventId")
    UUID eventId();

    @JsonProperty("eventName")
    String eventName();

    @JsonProperty("eventVersion")
    default String eventVersion() {
        return EVENT_VERSION;
    }

    @JsonProperty("occurredOn")
    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "America/La_Paz")
    Instant occurredOn();

    @JsonProperty("source")
    default String source() {
        return SOURCE;
    }

    @JsonProperty("correlationId")
    String correlationId();

    /**
     * Transport routing metadata; it is deliberately not part of the wire payload.
     */
    @JsonIgnore
    String logicalDestination();
}
