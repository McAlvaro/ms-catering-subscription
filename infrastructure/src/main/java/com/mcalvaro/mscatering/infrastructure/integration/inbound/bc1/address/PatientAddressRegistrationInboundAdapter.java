package com.mcalvaro.mscatering.infrastructure.integration.inbound.bc1.address;

import an.awesome.pipelinr.Pipeline;
import com.mcalvaro.mscatering.application.patient.address.synchronization.SynchronizePatientAddressCommand;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Anti-corruption adapter for BC1 address events; intentionally
 * transport-neutral.
 */
@Component
public class PatientAddressRegistrationInboundAdapter {

    private static final String EVENT_NAME = "bc1.DireccionClienteRegistrada";
    private static final String EVENT_VERSION = "1.0";
    private static final String SOURCE = "MsPatientsConsulCore";
    private final Pipeline pipeline;

    public PatientAddressRegistrationInboundAdapter(Pipeline pipeline) {
        this.pipeline = pipeline;
    }

    public void receive(PatientAddressRegisteredEvent event) {
        validate(event);
        PatientAddressRegisteredData data = event.data();
        pipeline.send(new SynchronizePatientAddressCommand(event.eventId(), event.eventName(), event.eventVersion(),
                event.occurredOn(), event.source(), event.correlationId(), data.patientId(), data.addressId(), data.alias(),
                data.street(), data.reference(), data.city(), data.geolocation().latitude(),
                data.geolocation().longitude(), payloadFingerprint(data)));
    }

    private void validate(PatientAddressRegisteredEvent event) {
        if (event == null || event.eventId() == null || !EVENT_NAME.equals(event.eventName())
                || !EVENT_VERSION.equals(event.eventVersion()) || event.occurredOn() == null
                || !SOURCE.equals(event.source()) || blank(event.correlationId()) || event.data() == null) {
            invalidContract();
        }
        PatientAddressRegisteredData data = event.data();
        if (data.patientId() == null || data.addressId() == null || blank(data.alias()) || blank(data.street()) || blank(data.reference())
                || blank(data.city())
                || data.geolocation() == null || !coordinate(data.geolocation().latitude(), -90, 90)
                || !coordinate(data.geolocation().longitude(), -180, 180)) {
            invalidContract();
        }
    }

    private boolean coordinate(Double value, double min, double max) {
        return value != null && Double.isFinite(value) && value >= min && value <= max;
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private String payloadFingerprint(PatientAddressRegisteredData data) {
        return sha256(data.patientId().toString(), data.addressId().toString(), data.alias(), data.street(),
                data.reference(), data.city(), data.geolocation().latitude().toString(),
                data.geolocation().longitude().toString());
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
        throw new PatientAddressRegistrationContractException("PROJ-001",
                "Invalid bc1.DireccionClienteRegistrada canonical contract");
    }
}
