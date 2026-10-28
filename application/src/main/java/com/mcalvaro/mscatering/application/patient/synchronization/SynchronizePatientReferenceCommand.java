package com.mcalvaro.mscatering.application.patient.synchronization;

import an.awesome.pipelinr.Command;

import java.time.Instant;
import java.util.UUID;

/**
 * Local, PII-free command for synchronizing BC3's patient eligibility
 * reference.
 */
public record SynchronizePatientReferenceCommand(
        UUID eventId,
        String eventName,
        String eventVersion,
        Instant occurredOn,
        String source,
        String correlationId,
        UUID patientId,
        String payloadFingerprint) implements Command<Void> {
}
