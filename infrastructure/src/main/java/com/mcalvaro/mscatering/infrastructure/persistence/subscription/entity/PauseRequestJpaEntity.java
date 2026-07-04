package com.mcalvaro.mscatering.infrastructure.persistence.subscription.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.FetchType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "pause_requests")
public class PauseRequestJpaEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private SubscriptionJpaEntity subscription;

    @Column(name = "range_start", nullable = false)
    private LocalDate rangeStart;

    @Column(name = "range_end", nullable = false)
    private LocalDate rangeEnd;

    @Column(name = "reason", nullable = false)
    private String reason;

    @Column(name = "actual_end_date")
    private LocalDate actualEndDate;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "active", nullable = false)
    private boolean active;

    public PauseRequestJpaEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public SubscriptionJpaEntity getSubscription() {
        return subscription;
    }

    public void setSubscription(SubscriptionJpaEntity subscription) {
        this.subscription = subscription;
    }

    public LocalDate getRangeStart() {
        return rangeStart;
    }

    public void setRangeStart(LocalDate rangeStart) {
        this.rangeStart = rangeStart;
    }

    public LocalDate getRangeEnd() {
        return rangeEnd;
    }

    public void setRangeEnd(LocalDate rangeEnd) {
        this.rangeEnd = rangeEnd;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LocalDate getActualEndDate() {
        return actualEndDate;
    }

    public void setActualEndDate(LocalDate actualEndDate) {
        this.actualEndDate = actualEndDate;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
