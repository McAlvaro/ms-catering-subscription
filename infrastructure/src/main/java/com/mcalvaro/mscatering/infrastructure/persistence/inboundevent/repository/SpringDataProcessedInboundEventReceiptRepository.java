package com.mcalvaro.mscatering.infrastructure.persistence.inboundevent.repository;

import com.mcalvaro.mscatering.infrastructure.persistence.inboundevent.entity.ProcessedInboundEventReceiptJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringDataProcessedInboundEventReceiptRepository
        extends JpaRepository<ProcessedInboundEventReceiptJpaEntity, UUID> {

    boolean existsByPatientId(UUID patientId);

    boolean existsByPatientIdAndPayloadFingerprint(UUID patientId, String payloadFingerprint);

    boolean existsByAddressIdAndPayloadFingerprint(UUID addressId, String payloadFingerprint);

    boolean existsBySubjectKindAndSubjectIdAndPayloadFingerprint(String subjectKind, UUID subjectId, String payloadFingerprint);
}
