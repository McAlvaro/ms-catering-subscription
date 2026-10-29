package com.mcalvaro.mscatering.infrastructure.support;

import com.mcalvaro.mscatering.infrastructure.integration.inbound.bc1.address.PatientAddressRegisteredData;
import com.mcalvaro.mscatering.infrastructure.integration.inbound.bc1.address.PatientAddressRegisteredEvent;
import com.mcalvaro.mscatering.infrastructure.integration.inbound.bc1.address.PatientAddressRegistrationInboundAdapter;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.UUID;

/**
 * Test-only BC1 address setup that exercises the production inbound adapter.
 */
public class PatientAddressRegistrationFixture {
    private final PatientAddressRegistrationInboundAdapter adapter;

    @Autowired
    public PatientAddressRegistrationFixture(PatientAddressRegistrationInboundAdapter adapter) {
        this.adapter = adapter;
    }

    public UUID ingestAddress(UUID patientId) {
        UUID addressId = UUID.randomUUID();
        ingestAddress(patientId, addressId);
        return addressId;
    }

    public void ingestAddress(UUID patientId, UUID addressId) {
        ingest(new PatientAddressRegisteredEvent(UUID.randomUUID(), "bc1.DireccionClienteRegistrada", "1.0",
                Instant.parse("2026-10-27T12:00:00Z"), "MsPatientsConsulCore", "test-" + UUID.randomUUID(),
                new PatientAddressRegisteredData(patientId, addressId, "Home", "Main Street", "Near park", "Cochabamba",
                        new PatientAddressRegisteredData.Geolocation(-17.3935, -66.1570))));
    }

    public void ingest(PatientAddressRegisteredEvent event) {
        adapter.receive(event);
    }
}
