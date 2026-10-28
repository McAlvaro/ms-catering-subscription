package com.mcalvaro.mscatering.infrastructure.integration.inbound.bc1.patient;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

/**
 * Consumed BC1 patient-registration event contract received by the future
 * broker adapter.
 */
public record PatientRegisteredEvent(
        UUID eventId,
        String eventName,
        String eventVersion,
        Instant occurredOn,
        String source,
        String correlationId,
        @JsonProperty("data") PatientRegisteredData data) {
}
