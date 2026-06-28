package com.mcalvaro.subscription.entity;

import com.mcalvaro.core.Entity;
import com.mcalvaro.subscription.vo.PauseRange;

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
    private LocalDate actualEndDate;
    private boolean active;

    public PauseRequest(UUID id, PauseRange range, String reason) {
        super(id);
        this.range = range;
        this.reason = reason;
        this.createdAt = Instant.now();
        this.active = true;
    }

    /**
     * Ends the pause early at the given reactivation date.
     */
    public void earlyReactivate(LocalDate reactivationDate) {
        this.actualEndDate = reactivationDate;
        this.active = false;
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
}
