package com.mcalvaro.mscatering.infrastructure.persistence.outbox.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_messages")
public class OutboxMessageJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;
    @Column(name = "type", nullable = false, length = 128)
    private String type;
    @Lob
    @Column(name = "content", nullable = false)
    private String content;
    @Column(name = "created", nullable = false)
    private Instant created;
    @Column(name = "processed", nullable = false)
    private boolean processed;
    @Column(name = "processed_on")
    private Instant processedOn;
    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;
    @Lob
    @Column(name = "last_error")
    private String lastError;
    @Column(name = "available_at", nullable = false)
    private Instant availableAt;
    @Column(name = "locked_until")
    private Instant lockedUntil;
    @Column(name = "locked_by", length = 128)
    private String lockedBy;
    @Column(name = "correlation_id", length = 255)
    private String correlationId;
    @Column(name = "trace_id", length = 64)
    private String traceId;
    @Column(name = "span_id", length = 32)
    private String spanId;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Instant getCreated() {
        return created;
    }

    public void setCreated(Instant created) {
        this.created = created;
    }

    public boolean isProcessed() {
        return processed;
    }

    public void setProcessed(boolean processed) {
        this.processed = processed;
    }

    public Instant getProcessedOn() {
        return processedOn;
    }

    public void setProcessedOn(Instant processedOn) {
        this.processedOn = processedOn;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public void setAttemptCount(int attemptCount) {
        this.attemptCount = attemptCount;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public Instant getAvailableAt() {
        return availableAt;
    }

    public void setAvailableAt(Instant availableAt) {
        this.availableAt = availableAt;
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }

    public void setLockedUntil(Instant lockedUntil) {
        this.lockedUntil = lockedUntil;
    }

    public String getLockedBy() {
        return lockedBy;
    }

    public void setLockedBy(String lockedBy) {
        this.lockedBy = lockedBy;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getSpanId() {
        return spanId;
    }

    public void setSpanId(String spanId) {
        this.spanId = spanId;
    }
}
