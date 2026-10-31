package com.mcalvaro.mscatering.infrastructure.persistence.patientaddress.synchronization;

import com.mcalvaro.mscatering.application.patient.synchronization.PatientReferenceSynchronizationException;
import com.mcalvaro.mscatering.infrastructure.BaseIntegrationTest;
import com.mcalvaro.mscatering.infrastructure.integration.inbound.bc1.address.PatientAddressRegisteredData;
import com.mcalvaro.mscatering.infrastructure.integration.inbound.bc1.address.PatientAddressRegisteredEvent;
import com.mcalvaro.mscatering.infrastructure.persistence.inboundevent.repository.SpringDataProcessedInboundEventReceiptRepository;
import com.mcalvaro.mscatering.infrastructure.persistence.patientaddress.repository.SpringDataPatientAddressReferenceRepository;
import com.mcalvaro.mscatering.infrastructure.support.PatientAddressRegistrationFixture;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional(propagation = Propagation.NOT_SUPPORTED)
class JpaPatientAddressSynchronizationStoreIT extends BaseIntegrationTest {

    @Autowired
    private PatientAddressRegistrationFixture fixture;
    @Autowired
    private SpringDataPatientAddressReferenceRepository addressRepository;
    @Autowired
    private SpringDataProcessedInboundEventReceiptRepository receiptRepository;

    @Test
    void permitsMultipleAddressesForOnePatientAndRecordsTheirReceipts() {
        String patientId = "PAC-" + UUID.randomUUID();
        PatientAddressRegisteredEvent first = event(UUID.randomUUID(), patientId, "DIR-" + UUID.randomUUID(), "One");
        PatientAddressRegisteredEvent second = event(UUID.randomUUID(), patientId, "DIR-" + UUID.randomUUID(), "Two");

        fixture.ingest(first);
        fixture.ingest(second);

        assertThat(addressRepository.count()).isGreaterThanOrEqualTo(2);
        assertThat(receiptRepository.findById(first.eventId())).isPresent();
        assertThat(receiptRepository.findById(second.eventId())).isPresent();
    }

    @Test
    void treatsExactDuplicateAsNoOpAndSemanticDuplicateAsAnotherReceipt() {
        String patientId = "PAC-" + UUID.randomUUID();
        String addressId = "DIR-" + UUID.randomUUID();
        PatientAddressRegisteredEvent first = event(UUID.randomUUID(), patientId, addressId, "One");
        PatientAddressRegisteredEvent semanticDuplicate = event(UUID.randomUUID(), patientId, addressId, "One");
        long receipts = receiptRepository.count();

        fixture.ingest(first);
        fixture.ingest(first);
        fixture.ingest(semanticDuplicate);

        assertThat(addressRepository.findById(addressId)).isPresent();
        assertThat(receiptRepository.count()).isEqualTo(receipts + 2);
    }

    @Test
    void rejectsChangedContentForTheSameEventOrAddressWithoutOverwrite() {
        String patientId = "PAC-" + UUID.randomUUID();
        String addressId = "DIR-" + UUID.randomUUID();
        PatientAddressRegisteredEvent first = event(UUID.randomUUID(), patientId, addressId, "One");
        fixture.ingest(first);

        assertThatThrownBy(() -> fixture.ingest(event(first.eventId(), patientId, addressId, "Two")))
                .isInstanceOf(PatientReferenceSynchronizationException.class)
                .hasFieldOrPropertyWithValue("code", "PROJ-002");
        assertThatThrownBy(() -> fixture.ingest(event(UUID.randomUUID(), patientId, addressId, "Two")))
                .isInstanceOf(PatientReferenceSynchronizationException.class)
                .hasFieldOrPropertyWithValue("code", "PROJ-002");
        assertThat(addressRepository.findById(addressId).orElseThrow().getStreet()).isEqualTo("One");
    }

    @Test
    void rollsBackAddressWhenReceiptCannotBePersisted() {
        String addressId = "DIR-" + UUID.randomUUID();
        PatientAddressRegisteredEvent invalid = new PatientAddressRegisteredEvent(UUID.randomUUID(),
                "bc1.DireccionClienteRegistrada", "1.0", Instant.now(), "MsPatientsConsulCore", "x".repeat(256),
                new PatientAddressRegisteredData("PAC-" + UUID.randomUUID(), addressId, "Home", "One", "Reference", "City",
                        new PatientAddressRegisteredData.Geolocation(0d, 0d)));

        assertThatThrownBy(() -> fixture.ingest(invalid)).isInstanceOf(DataIntegrityViolationException.class);
        assertThat(addressRepository.findById(addressId)).isEmpty();
        assertThat(receiptRepository.findById(invalid.eventId())).isEmpty();
    }

    private PatientAddressRegisteredEvent event(UUID eventId, String patientId, String addressId, String street) {
        return new PatientAddressRegisteredEvent(eventId, "bc1.DireccionClienteRegistrada", "1.0",
                Instant.parse("2026-10-27T12:00:00Z"), "MsPatientsConsulCore", "correlation-" + eventId,
                new PatientAddressRegisteredData(patientId, addressId, "Home", street, "Reference", "City",
                        new PatientAddressRegisteredData.Geolocation(-17.39, -66.15)));
    }
}
