package com.mcalvaro.mscatering.application.patient.synchronization;

import an.awesome.pipelinr.Command;
import com.mcalvaro.mscatering.domain.core.id.PatientId;

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
        PatientId patientId,
        String patientName,
        String payloadFingerprint) implements Command<Void> {

    public SynchronizePatientReferenceCommand(
            UUID eventId,
            String eventName,
            String eventVersion,
            Instant occurredOn,
            String source,
            String correlationId,
            String patientId,
            String patientName,
            String payloadFingerprint) {
        this(eventId, eventName, eventVersion, occurredOn, source, correlationId,
                PatientId.of(patientId), patientName, payloadFingerprint);
    }
}
