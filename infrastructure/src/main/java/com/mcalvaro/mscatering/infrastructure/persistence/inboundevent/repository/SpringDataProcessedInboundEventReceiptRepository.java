package com.mcalvaro.mscatering.infrastructure.persistence.inboundevent.repository;

import com.mcalvaro.mscatering.infrastructure.persistence.inboundevent.entity.ProcessedInboundEventReceiptJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringDataProcessedInboundEventReceiptRepository
        extends JpaRepository<ProcessedInboundEventReceiptJpaEntity, UUID> {

    boolean existsByPatientId(String patientId);

    boolean existsByPatientIdAndPayloadFingerprint(String patientId, String payloadFingerprint);

    boolean existsByAddressIdAndPayloadFingerprint(String addressId, String payloadFingerprint);

    boolean existsBySubjectKindAndSubjectIdAndPayloadFingerprint(String subjectKind, String subjectId, String payloadFingerprint);
}
