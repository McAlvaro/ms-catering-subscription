package com.mcalvaro.mscatering.domain.subscription.event;

import com.mcalvaro.mscatering.domain.core.CorrelatedDomainEvent;
import com.mcalvaro.mscatering.domain.subscription.vo.PauseRange;

import java.time.Instant;
import java.util.UUID;

public record SubscriptionPaused(
        UUID eventId,
        Instant occurredAt,
        UUID subscriptionId,
        UUID patientId,
        PauseRange pauseRange,
        String reason,
        java.time.LocalDate requestedOn,
        java.time.LocalDate newEndDate,
        String correlationId) implements CorrelatedDomainEvent {

    public SubscriptionPaused(UUID subscriptionId, UUID patientId, PauseRange pauseRange, String reason,
            java.time.LocalDate requestedOn, java.time.LocalDate newEndDate, String correlationId) {
        this(UUID.randomUUID(), Instant.now(), subscriptionId, patientId, pauseRange, reason, requestedOn, newEndDate,
                correlationId);
    }
}
