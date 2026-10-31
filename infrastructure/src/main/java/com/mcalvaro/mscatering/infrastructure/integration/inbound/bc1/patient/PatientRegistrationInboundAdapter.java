package com.mcalvaro.mscatering.infrastructure.integration.inbound.bc1.patient;

import an.awesome.pipelinr.Pipeline;
import com.mcalvaro.mscatering.application.patient.synchronization.SynchronizePatientReferenceCommand;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Infrastructure anti-corruption layer and future broker entry point for BC1
 * patient-registration events.
 */
@Component
public class PatientRegistrationInboundAdapter {

    private static final String EVENT_NAME = "bc1.PacienteRegistrado";
    private static final String EVENT_VERSION = "1.0";
    private static final String SOURCE = "MsPatientsConsulCore";

    private final Pipeline pipeline;

    public PatientRegistrationInboundAdapter(Pipeline pipeline) {
        this.pipeline = pipeline;
    }

    public void receive(PatientRegisteredEvent event) {
        validate(event);
        PatientRegisteredData data = event.data();
        pipeline.send(new SynchronizePatientReferenceCommand(
                event.eventId(), event.eventName(), event.eventVersion(), event.occurredOn(), event.source(),
                event.correlationId(), data.patientId(), data.patientName(), payloadFingerprint(data)));
    }

    private void validate(PatientRegisteredEvent event) {
        if (event == null
                || event.eventId() == null
                || !EVENT_NAME.equals(event.eventName())
                || !EVENT_VERSION.equals(event.eventVersion())
                || event.occurredOn() == null
                || !SOURCE.equals(event.source())
                || isBlank(event.correlationId())
                || event.data() == null) {
            invalidContract();
        }

        if (isBlank(event.data().patientId()) || isBlank(event.data().patientName())) {
            invalidContract();
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String payloadFingerprint(PatientRegisteredData data) {
        return sha256(data.patientId(), data.patientName());
    }

    private String sha256(String... values) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (String value : values) {
                byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
                digest.update((byte) (bytes.length >>> 24));
                digest.update((byte) (bytes.length >>> 16));
                digest.update((byte) (bytes.length >>> 8));
                digest.update((byte) bytes.length);
                digest.update(bytes);
            }
            return java.util.HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 must be available", exception);
        }
    }

    private void invalidContract() {
        throw new PatientRegistrationContractException("PROJ-001",
                "Invalid bc1.PacienteRegistrado canonical contract");
    }
}
