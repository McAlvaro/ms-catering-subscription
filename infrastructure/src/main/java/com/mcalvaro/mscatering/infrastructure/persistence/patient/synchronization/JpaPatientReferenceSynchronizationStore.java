package com.mcalvaro.mscatering.infrastructure.persistence.patient.synchronization;

import com.mcalvaro.mscatering.application.patient.synchronization.PatientReferenceSynchronizationException;
import com.mcalvaro.mscatering.application.patient.synchronization.PatientReferenceSynchronizationStore;
import com.mcalvaro.mscatering.application.patient.synchronization.SynchronizePatientReferenceCommand;
import com.mcalvaro.mscatering.infrastructure.persistence.inboundevent.entity.ProcessedInboundEventReceiptJpaEntity;
import com.mcalvaro.mscatering.infrastructure.persistence.inboundevent.entity.ReceiptSubjectKind;
import com.mcalvaro.mscatering.infrastructure.persistence.inboundevent.repository.SpringDataProcessedInboundEventReceiptRepository;
import com.mcalvaro.mscatering.infrastructure.persistence.patient.entity.PatientReferenceJpaEntity;
import com.mcalvaro.mscatering.infrastructure.persistence.patient.repository.SpringDataPatientReferenceRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** JPA adapter implementing atomic receipt and patient-reference semantics. */
@Repository
public class JpaPatientReferenceSynchronizationStore implements PatientReferenceSynchronizationStore {

    private final SpringDataPatientReferenceRepository patientReferenceRepository;
    private final SpringDataProcessedInboundEventReceiptRepository receiptRepository;

    public JpaPatientReferenceSynchronizationStore(SpringDataPatientReferenceRepository patientReferenceRepository,
            SpringDataProcessedInboundEventReceiptRepository receiptRepository) {
        this.patientReferenceRepository = patientReferenceRepository;
        this.receiptRepository = receiptRepository;
    }

    @Override
    @Transactional
    public void synchronize(SynchronizePatientReferenceCommand command) {
        String payloadFingerprint = command.payloadFingerprint();
        String eventFingerprint = eventFingerprint(command, payloadFingerprint);

        var previousReceipt = receiptRepository.findById(command.eventId());
        if (previousReceipt.isPresent()) {
            if (previousReceipt.get().getEventFingerprint().equals(eventFingerprint)) {
                return;
            }
            throw conflict("Inbound eventId was reused with different canonical content");
        }

        if (receiptRepository.existsBySubjectKindAndSubjectIdAndPayloadFingerprint(ReceiptSubjectKind.PATIENT.name(), command.patientId(), payloadFingerprint)) {
            receiptRepository.saveAndFlush(receipt(command, eventFingerprint, payloadFingerprint));
            return;
        }
        if (receiptRepository.existsByPatientId(command.patientId())) {
            throw conflict("Patient identity already has different canonical content");
        }

        patientReferenceRepository.save(newPatientReference(command));
        receiptRepository.saveAndFlush(receipt(command, eventFingerprint, payloadFingerprint));
    }

    private PatientReferenceJpaEntity newPatientReference(SynchronizePatientReferenceCommand command) {
        PatientReferenceJpaEntity reference = new PatientReferenceJpaEntity();
        reference.setPatientId(command.patientId());
        reference.setPatientName(command.patientName());
        reference.setActive(true);
        reference.setUpdatedAt(command.occurredOn());
        return reference;
    }

    private ProcessedInboundEventReceiptJpaEntity receipt(SynchronizePatientReferenceCommand command,
            String eventFingerprint, String payloadFingerprint) {
        return new ProcessedInboundEventReceiptJpaEntity(command.eventId(), command.eventName(),
                command.eventVersion(), command.occurredOn(), command.source(), command.correlationId(),
                command.patientId(), eventFingerprint, payloadFingerprint);
    }

    private PatientReferenceSynchronizationException conflict(String message) {
        return new PatientReferenceSynchronizationException("PROJ-002", message);
    }

    private String eventFingerprint(SynchronizePatientReferenceCommand command, String payloadFingerprint) {
        return sha256(command.eventName(), command.eventVersion(), command.occurredOn().toString(),
                command.source(), command.correlationId(), payloadFingerprint);
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
