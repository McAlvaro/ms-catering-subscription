package com.mcalvaro.mscatering.infrastructure.support;

import com.mcalvaro.mscatering.infrastructure.integration.inbound.bc1.patient.PatientRegisteredEvent;
import com.mcalvaro.mscatering.infrastructure.integration.inbound.bc1.patient.PatientRegisteredData;
import com.mcalvaro.mscatering.infrastructure.integration.inbound.bc1.patient.PatientRegistrationInboundAdapter;
import com.mcalvaro.mscatering.infrastructure.persistence.patient.repository.SpringDataPatientReferenceRepository;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.UUID;

/** Test-only patient state setup that exercises the production inbound adapter. */
public class PatientRegistrationFixture {

    private final PatientRegistrationInboundAdapter adapter;
    private final SpringDataPatientReferenceRepository patientReferenceRepository;

    @Autowired
    public PatientRegistrationFixture(PatientRegistrationInboundAdapter adapter,
            SpringDataPatientReferenceRepository patientReferenceRepository) {
        this.adapter = adapter;
        this.patientReferenceRepository = patientReferenceRepository;
    }

    public void ingestEligiblePatient(String patientId) {
        adapter.receive(eventFor(patientId));
    }

    public void ingest(PatientRegisteredEvent event) {
        adapter.receive(event);
    }

    public void ingestIneligiblePatient(String patientId) {
        ingestEligiblePatient(patientId);
        var reference = patientReferenceRepository.findById(patientId).orElseThrow();
        reference.setActive(false);
        patientReferenceRepository.saveAndFlush(reference);
    }

    public PatientRegisteredEvent eventFor(String patientId) {
        return new PatientRegisteredEvent(
                UUID.randomUUID(),
                "bc1.PacienteRegistrado",
                "1.0",
                Instant.parse("2026-10-27T12:00:00Z"),
                "MsPatientsConsulCore",
                "test-correlation-" + UUID.randomUUID(),
                new PatientRegisteredData(patientId, "Test patient"));
    }
}
