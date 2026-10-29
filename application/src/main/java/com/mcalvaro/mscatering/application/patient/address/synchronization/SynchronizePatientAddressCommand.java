package com.mcalvaro.mscatering.application.patient.address.synchronization;

import an.awesome.pipelinr.Command;

import java.time.Instant;
import java.util.UUID;

/** The minimum BC1 address data BC3 consumes and persists. */
public record SynchronizePatientAddressCommand(
        UUID eventId,
        String eventName,
        String eventVersion,
        Instant occurredOn,
        String source,
        String correlationId,
        UUID patientId,
        UUID addressId,
        String alias,
        String street,
        String reference,
        String city,
        double latitude,
        double longitude,
        String payloadFingerprint) implements Command<Void> {
}
