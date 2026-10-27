package com.mcalvaro.mscatering.domain.subscription.entity;

import com.mcalvaro.mscatering.domain.core.Entity;
import com.mcalvaro.mscatering.domain.subscription.vo.PauseRange;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Entity representing a formal pause request for a subscription
 * (SolicitudDePausa).
 */
public class PauseRequest extends Entity {

    private final PauseRange range;
    private final String reason;
    private final Instant createdAt;
    private final String correlationId;
    private LocalDate actualEndDate;
    private int extensionDays;
    private boolean active;

    public PauseRequest(UUID id, PauseRange range, String reason) {
        this(id, range, reason, UUID.randomUUID().toString(), range.durationDays());
    }

    public PauseRequest(UUID id, PauseRange range, String reason, String correlationId, int extensionDays) {
        super(id);
        this.range = range;
        this.reason = reason;
        this.createdAt = Instant.now();
        if (correlationId == null || correlationId.isBlank()) {
            throw new IllegalArgumentException("PauseRequest correlationId must not be blank");
        }
        this.correlationId = correlationId;
        this.extensionDays = extensionDays;
        this.active = true;
    }

    /**
     * Ends the pause early at the given reactivation date.
     */
    public void earlyReactivate(LocalDate reactivationDate) {
        this.actualEndDate = reactivationDate;
        this.active = false;
    }

    public void reduceExtensionDays(int days) {
        this.extensionDays -= days;
    }

    public PauseRange getRange() {
        return range;
    }

    public String getReason() {
        return reason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public LocalDate getActualEndDate() {
        return actualEndDate;
    }

    public boolean isActive() {
        return active;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public int getExtensionDays() {
        return extensionDays;
    }
}
