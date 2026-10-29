package com.mcalvaro.mscatering.infrastructure.persistence.patientaddress.synchronization;

import com.mcalvaro.mscatering.application.patient.address.synchronization.PatientAddressSynchronizationStore;
import com.mcalvaro.mscatering.application.patient.address.synchronization.SynchronizePatientAddressCommand;
import com.mcalvaro.mscatering.application.patient.synchronization.PatientReferenceSynchronizationException;
import com.mcalvaro.mscatering.infrastructure.persistence.inboundevent.entity.ProcessedInboundEventReceiptJpaEntity;
import com.mcalvaro.mscatering.infrastructure.persistence.inboundevent.entity.ReceiptSubjectKind;
import com.mcalvaro.mscatering.infrastructure.persistence.inboundevent.repository.SpringDataProcessedInboundEventReceiptRepository;
import com.mcalvaro.mscatering.infrastructure.persistence.patientaddress.entity.PatientAddressReferenceJpaEntity;
import com.mcalvaro.mscatering.infrastructure.persistence.patientaddress.repository.SpringDataPatientAddressReferenceRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Durable, atomic and idempotent address projection. */
@Repository
public class JpaPatientAddressSynchronizationStore implements PatientAddressSynchronizationStore {

    private final SpringDataPatientAddressReferenceRepository addressRepository;
    private final SpringDataProcessedInboundEventReceiptRepository receiptRepository;

    public JpaPatientAddressSynchronizationStore(SpringDataPatientAddressReferenceRepository addressRepository,
            SpringDataProcessedInboundEventReceiptRepository receiptRepository) {
        this.addressRepository = addressRepository;
        this.receiptRepository = receiptRepository;
    }

    @Override
    @Transactional
    public synchronized void synchronize(SynchronizePatientAddressCommand command) {
        String eventFingerprint = fingerprint(command, command.payloadFingerprint());
        var previousReceipt = receiptRepository.findById(command.eventId());
        if (previousReceipt.isPresent()) {
            if (previousReceipt.get().getEventFingerprint().equals(eventFingerprint))
                return;
            throw conflict("Inbound eventId was reused with different canonical content");
        }
        if (receiptRepository.existsBySubjectKindAndSubjectIdAndPayloadFingerprint(ReceiptSubjectKind.PATIENT_ADDRESS.name(), command.addressId(),
                command.payloadFingerprint())) {
            receiptRepository.saveAndFlush(receipt(command, eventFingerprint));
            return;
        }
        if (addressRepository.existsById(command.addressId())) {
            throw conflict("Address identity already has different canonical content");
        }
        addressRepository.save(new PatientAddressReferenceJpaEntity(command.addressId(), command.patientId(), command.alias(),
                command.street(), command.reference(), command.city(), command.latitude(), command.longitude()));
        receiptRepository.saveAndFlush(receipt(command, eventFingerprint));
    }

    private ProcessedInboundEventReceiptJpaEntity receipt(SynchronizePatientAddressCommand command,
            String eventFingerprint) {
        return new ProcessedInboundEventReceiptJpaEntity(command.eventId(), command.eventName(), command.eventVersion(),
                command.occurredOn(), command.source(), command.correlationId(), command.patientId(),
                command.addressId(),
                eventFingerprint, command.payloadFingerprint());
    }

    private PatientReferenceSynchronizationException conflict(String message) {
        return new PatientReferenceSynchronizationException("PROJ-002", message);
    }

    private String fingerprint(SynchronizePatientAddressCommand command, String payloadFingerprint) {
        return sha256(command.eventName(), command.eventVersion(), command.occurredOn().toString(), command.source(),
                command.correlationId(), payloadFingerprint);
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
}
