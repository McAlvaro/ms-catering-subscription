package com.mcalvaro.mscatering.infrastructure.persistence.inboundevent.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "processed_inbound_event_receipts")
public class ProcessedInboundEventReceiptJpaEntity {

    @Id
    @Column(name = "event_id", updatable = false, nullable = false)
    private UUID eventId;

    @Column(name = "event_name", nullable = false, length = 100)
    private String eventName;

    @Column(name = "event_version", nullable = false, length = 20)
    private String eventVersion;

    @Column(name = "occurred_on", nullable = false)
    private Instant occurredOn;

    @Column(name = "source", nullable = false, length = 100)
    private String source;

    @Column(name = "correlation_id", nullable = false, length = 255)
    private String correlationId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "address_id")
    private UUID addressId;

    @Column(name = "subject_kind", nullable = false, length = 30)
    private String subjectKind;

    @Column(name = "subject_id", nullable = false)
    private UUID subjectId;

    @Column(name = "event_fingerprint", nullable = false, length = 64)
    private String eventFingerprint;

    @Column(name = "payload_fingerprint", nullable = false, length = 64)
    private String payloadFingerprint;

    public ProcessedInboundEventReceiptJpaEntity() {
    }

    public ProcessedInboundEventReceiptJpaEntity(UUID eventId, String eventName, String eventVersion,
            Instant occurredOn,
            String source, String correlationId, UUID patientId, String eventFingerprint, String payloadFingerprint) {
        this(eventId, eventName, eventVersion, occurredOn, source, correlationId, patientId, null, eventFingerprint,
                payloadFingerprint);
    }

    public ProcessedInboundEventReceiptJpaEntity(UUID eventId, String eventName, String eventVersion,
            Instant occurredOn, String source, String correlationId, UUID patientId, UUID addressId,
            String eventFingerprint, String payloadFingerprint) {
        this.eventId = eventId;
        this.eventName = eventName;
        this.eventVersion = eventVersion;
        this.occurredOn = occurredOn;
        this.source = source;
        this.correlationId = correlationId;
        this.patientId = patientId;
        this.addressId = addressId;
        this.subjectKind = addressId == null ? ReceiptSubjectKind.PATIENT.name() : ReceiptSubjectKind.PATIENT_ADDRESS.name();
        this.subjectId = addressId == null ? patientId : addressId;
        this.eventFingerprint = eventFingerprint;
        this.payloadFingerprint = payloadFingerprint;
    }

    public ProcessedInboundEventReceiptJpaEntity(UUID eventId, String eventName, String eventVersion, Instant occurredOn,
            String source, String correlationId, ReceiptSubjectKind subjectKind, UUID subjectId, String eventFingerprint,
            String payloadFingerprint) {
        this.eventId = eventId; this.eventName = eventName; this.eventVersion = eventVersion; this.occurredOn = occurredOn;
        this.source = source; this.correlationId = correlationId; this.subjectKind = subjectKind.name(); this.subjectId = subjectId;
        this.eventFingerprint = eventFingerprint; this.payloadFingerprint = payloadFingerprint;
    }

    public UUID getEventId() {
        return eventId;
    }

    public String getEventFingerprint() {
        return eventFingerprint;
    }

    public String getPayloadFingerprint() {
        return payloadFingerprint;
    }
}
