package com.mcalvaro.mscatering.infrastructure.persistence.patient.synchronization;

import com.mcalvaro.mscatering.application.patient.synchronization.PatientReferenceSynchronizationException;
import com.mcalvaro.mscatering.infrastructure.BaseIntegrationTest;
import com.mcalvaro.mscatering.infrastructure.integration.inbound.bc1.patient.PatientRegisteredData;
import com.mcalvaro.mscatering.infrastructure.integration.inbound.bc1.patient.PatientRegisteredEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcalvaro.mscatering.infrastructure.persistence.inboundevent.repository.SpringDataProcessedInboundEventReceiptRepository;
import com.mcalvaro.mscatering.infrastructure.persistence.patient.entity.PatientReferenceJpaEntity;
import com.mcalvaro.mscatering.infrastructure.persistence.patient.repository.SpringDataPatientReferenceRepository;
import com.mcalvaro.mscatering.infrastructure.support.PatientRegistrationFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional(propagation = Propagation.NOT_SUPPORTED)
class JpaPatientReferenceSynchronizationStoreIT extends BaseIntegrationTest {

    @Autowired
    private PatientRegistrationFixture fixture;

    @Autowired
    private SpringDataPatientReferenceRepository patientReferenceRepository;

    @Autowired
    private SpringDataProcessedInboundEventReceiptRepository receiptRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    @DisplayName("Persists only the eligibility reference and receipt after accepting the complete payload")
    void persistsOnlyReferenceAndReceipt() {
        PatientRegisteredEvent event = fixture.eventFor("PAC-" + UUID.randomUUID());

        fixture.ingest(event);

        PatientReferenceJpaEntity reference = patientReferenceRepository.findById(event.data().patientId()).orElseThrow();
        assertThat(reference.isActive()).isTrue();
        assertThat(reference.getUpdatedAt()).isEqualTo(event.occurredOn());
        var receipt = receiptRepository.findById(event.eventId()).orElseThrow();
        assertThat(receipt.getPayloadFingerprint()).matches("[0-9a-f]{64}");
        assertThat(receipt.getEventFingerprint()).matches("[0-9a-f]{64}");
    }

    @Test
    @DisplayName("Makes an exact event-id duplicate a no-op even when ignored PII changes")
    void ignoresExactDuplicateWhenIgnoredPiiChanges() throws Exception {
        UUID eventId = UUID.randomUUID();
        String patientId = "PAC-" + UUID.randomUUID();
        PatientRegisteredEvent event = eventFromJson(eventId, patientId, "Ana Uno", "ana@example.com");
        PatientRegisteredEvent piiOnlyChange = eventFromJson(eventId, patientId, "Ana Uno", "ana.dos@example.com");
        long referencesBefore = patientReferenceRepository.count();
        long receiptsBefore = receiptRepository.count();

        fixture.ingest(event);
        fixture.ingest(piiOnlyChange);

        assertThat(patientReferenceRepository.count()).isEqualTo(referencesBefore + 1);
        assertThat(receiptRepository.count()).isEqualTo(receiptsBefore + 1);
    }

    @Test
    @DisplayName("Records a semantic duplicate without overwriting when only ignored PII changes")
    void recordsSemanticDuplicateWithoutProjectionOverwriteWhenIgnoredPiiChanges() throws Exception {
        String patientId = "PAC-" + UUID.randomUUID();
        PatientRegisteredEvent first = eventFromJson(UUID.randomUUID(), patientId, "Ana Uno", "ana@example.com");
        PatientRegisteredEvent duplicate = eventFromJson(UUID.randomUUID(), patientId, "Ana Uno", "ana.dos@example.com");
        long receiptsBefore = receiptRepository.count();

        fixture.ingest(first);
        fixture.ingest(duplicate);

        assertThat(receiptRepository.count()).isEqualTo(receiptsBefore + 2);
        assertThat(patientReferenceRepository.findById(patientId).orElseThrow().getUpdatedAt())
                .isEqualTo(first.occurredOn());
    }

    @Test
    @DisplayName("Rejects an event-id reuse with changed canonical content without overwriting state")
    void rejectsEventIdConflictWithoutOverwrite() {
        PatientRegisteredEvent first = fixture.eventFor("PAC-" + UUID.randomUUID());
        fixture.ingest(first);
        PatientRegisteredEvent conflict = new PatientRegisteredEvent(
                first.eventId(), first.eventName(), first.eventVersion(), first.occurredOn(), first.source(),
                "changed-correlation", first.data());

        assertSynchronizationConflict(conflict);

        assertThat(patientReferenceRepository.findById(first.data().patientId()).orElseThrow().getUpdatedAt())
                .isEqualTo(first.occurredOn());
        assertThat(receiptRepository.existsById(first.eventId())).isTrue();
    }

    @Test
    @DisplayName("Rejects an event-id reuse with a different patient without overwriting state")
    void rejectsEventIdReuseWithDifferentPatient() {
        PatientRegisteredEvent first = fixture.eventFor("PAC-" + UUID.randomUUID());
        fixture.ingest(first);
        PatientRegisteredEvent conflict = new PatientRegisteredEvent(
                first.eventId(), first.eventName(), first.eventVersion(), first.occurredOn(), first.source(),
                first.correlationId(), new PatientRegisteredData("PAC-" + UUID.randomUUID(), "Conflicting patient"));

        assertSynchronizationConflict(conflict);

        assertThat(patientReferenceRepository.existsById(first.data().patientId())).isTrue();
        assertThat(patientReferenceRepository.existsById(conflict.data().patientId())).isFalse();
        assertThat(receiptRepository.existsById(first.eventId())).isTrue();
    }

    @Test
    @DisplayName("Rolls back reference and receipt together when receipt persistence fails")
    void rollsBackWhenDatabasePersistenceFails() {
        PatientRegisteredEvent event = fixture.eventFor("PAC-" + UUID.randomUUID());
        PatientRegisteredEvent tooLongCorrelationId = new PatientRegisteredEvent(
                event.eventId(), event.eventName(), event.eventVersion(), event.occurredOn(), event.source(),
                "x".repeat(256), event.data());

        assertThatThrownBy(() -> fixture.ingest(tooLongCorrelationId))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThat(patientReferenceRepository.existsById(event.data().patientId())).isFalse();
        assertThat(receiptRepository.existsById(event.eventId())).isFalse();
    }

    @Test
    @DisplayName("Does not create the patient profiles table")
    void doesNotCreatePatientProfilesTable() {
        Integer tables = jdbcTemplate.queryForObject("""
                select count(*)
                from information_schema.tables
                where upper(table_name) = 'PATIENT_PROFILES'
                """, Integer.class);
        var receiptColumns = jdbcTemplate.queryForList("""
                select upper(column_name)
                from information_schema.columns
                where upper(table_name) = 'PROCESSED_INBOUND_EVENT_RECEIPTS'
                """, String.class);

        assertThat(tables).isZero();
        assertThat(receiptColumns).doesNotContain(
                "FULL_NAME", "BIRTH_DATE", "SEX", "PHONE", "EMAIL", "NUTRITIONIST_ID", "REGISTRATION_DATE");
    }

    private void assertSynchronizationConflict(PatientRegisteredEvent event) {
        assertThatThrownBy(() -> fixture.ingest(event))
                .isInstanceOf(PatientReferenceSynchronizationException.class)
                .extracting("code")
                .isEqualTo("PROJ-002");
    }

    private PatientRegisteredEvent eventFromJson(UUID eventId, String patientId, String nombreCompleto, String email)
            throws Exception {
        return objectMapper.readValue("""
                {
                  "eventId": "%s",
                  "eventName": "bc1.PacienteRegistrado",
                  "eventVersion": "1.0",
                  "occurredOn": "2026-10-27T12:00:00Z",
                  "source": "MsPatientsConsulCore",
                  "correlationId": "test-correlation",
                  "data": {
                    "idPaciente": "%s",
                    "nombreCompleto": "%s",
                    "email": "%s"
                  }
                }
                """.formatted(eventId, patientId, nombreCompleto, email), PatientRegisteredEvent.class);
    }
}
