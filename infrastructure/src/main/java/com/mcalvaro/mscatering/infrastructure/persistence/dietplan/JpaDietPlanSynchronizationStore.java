package com.mcalvaro.mscatering.infrastructure.persistence.dietplan;

import com.mcalvaro.mscatering.application.dietplan.synchronization.*;
import com.mcalvaro.mscatering.application.patient.synchronization.PatientReferenceSynchronizationException;
import com.mcalvaro.mscatering.infrastructure.persistence.dietplan.repository.SpringDataDietPlanProjectionRepository;
import com.mcalvaro.mscatering.infrastructure.persistence.inboundevent.entity.ProcessedInboundEventReceiptJpaEntity;
import com.mcalvaro.mscatering.infrastructure.persistence.inboundevent.entity.ReceiptSubjectKind;
import com.mcalvaro.mscatering.infrastructure.persistence.inboundevent.repository.SpringDataProcessedInboundEventReceiptRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Repository
public class JpaDietPlanSynchronizationStore implements DietPlanSynchronizationStore {
    private final SpringDataDietPlanProjectionRepository plans; private final DietPlanProjectionMapper mapper;
    private final SpringDataProcessedInboundEventReceiptRepository receipts;
    public JpaDietPlanSynchronizationStore(SpringDataDietPlanProjectionRepository plans, DietPlanProjectionMapper mapper, SpringDataProcessedInboundEventReceiptRepository receipts) { this.plans = plans; this.mapper = mapper; this.receipts = receipts; }
    @Override @Transactional
    public void synchronize(SynchronizeDietPlanCommand command) {
        String eventFingerprint = fingerprint(command);
        var receipt = receipts.findById(command.eventId());
        if (receipt.isPresent()) { if (receipt.get().getEventFingerprint().equals(eventFingerprint)) return; throw conflict("Inbound eventId was reused with different canonical content"); }
        if (receipts.existsBySubjectKindAndSubjectIdAndPayloadFingerprint(ReceiptSubjectKind.DIET_PLAN.name(), command.plan().id().value(), command.payloadFingerprint())) { receipts.saveAndFlush(receipt(command, eventFingerprint)); return; }
        if (plans.existsById(command.plan().id().value())) throw conflict("Diet plan identity already has different canonical content");
        plans.save(mapper.toJpa(command.plan()));
        receipts.saveAndFlush(receipt(command, eventFingerprint));
    }
    private ProcessedInboundEventReceiptJpaEntity receipt(SynchronizeDietPlanCommand command, String fingerprint) { return new ProcessedInboundEventReceiptJpaEntity(command.eventId(), command.eventName(), command.eventVersion(), command.occurredOn(), command.source(), command.correlationId(), ReceiptSubjectKind.DIET_PLAN, command.plan().id().value(), fingerprint, command.payloadFingerprint()); }
    private String fingerprint(SynchronizeDietPlanCommand c) { return sha256(c.eventName(), c.eventVersion(), c.occurredOn().toString(), c.source(), c.correlationId(), c.payloadFingerprint()); }
    private String sha256(String... values) { try { MessageDigest digest = MessageDigest.getInstance("SHA-256"); for (String value : values) { byte[] bytes = value.getBytes(StandardCharsets.UTF_8); digest.update((byte)(bytes.length >>> 24)); digest.update((byte)(bytes.length >>> 16)); digest.update((byte)(bytes.length >>> 8)); digest.update((byte)bytes.length); digest.update(bytes); } return java.util.HexFormat.of().formatHex(digest.digest()); } catch (Exception e) { throw new IllegalStateException(e); } }
    private PatientReferenceSynchronizationException conflict(String message) { return new PatientReferenceSynchronizationException("PROJ-002", message); }
}
