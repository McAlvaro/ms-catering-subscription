package com.mcalvaro.mscatering.application.patient.address.synchronization;

import an.awesome.pipelinr.Command;
import com.mcalvaro.mscatering.domain.core.id.AddressId;
import com.mcalvaro.mscatering.domain.core.id.PatientId;

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
        PatientId patientId,
        AddressId addressId,
        String alias,
        String street,
        String reference,
        String city,
        double latitude,
        double longitude,
        String payloadFingerprint) implements Command<Void> {

    public SynchronizePatientAddressCommand(
            UUID eventId,
            String eventName,
            String eventVersion,
            Instant occurredOn,
            String source,
            String correlationId,
            String patientId,
            String addressId,
            String alias,
            String street,
            String reference,
            String city,
            double latitude,
            double longitude,
            String payloadFingerprint) {
        this(eventId, eventName, eventVersion, occurredOn, source, correlationId,
                PatientId.of(patientId), AddressId.of(addressId),
                alias, street, reference, city, latitude, longitude, payloadFingerprint);
    }
}
