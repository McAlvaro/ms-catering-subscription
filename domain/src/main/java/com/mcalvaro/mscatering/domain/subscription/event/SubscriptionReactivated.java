package com.mcalvaro.mscatering.domain.subscription.event;

import com.mcalvaro.mscatering.domain.core.CorrelatedDomainEvent;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record SubscriptionReactivated(
        UUID eventId,
        Instant occurredAt,
        UUID subscriptionId,
        UUID patientId,
        LocalDate reactivatedOn,
        LocalDate effectiveEndDate,
        String correlationId) implements CorrelatedDomainEvent {

    public SubscriptionReactivated(UUID subscriptionId, UUID patientId, LocalDate reactivatedOn,
            LocalDate effectiveEndDate, String correlationId) {
        this(UUID.randomUUID(), Instant.now(), subscriptionId, patientId, reactivatedOn, effectiveEndDate,
                correlationId);
    }
}
