package com.mcalvaro.mscatering.infrastructure.integration.inbound.bc1.address;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

/** BC1 v1.0 address-registration event envelope consumed by BC3. */
public record PatientAddressRegisteredEvent(
        UUID eventId,
        String eventName,
        String eventVersion,
        Instant occurredOn,
        String source,
        String correlationId,
        @JsonProperty("data") PatientAddressRegisteredData data) {
}
