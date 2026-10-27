package com.mcalvaro.mscatering.domain.subscription.event;

import com.mcalvaro.mscatering.domain.core.CorrelatedDomainEvent;

import java.time.Instant;
import java.util.UUID;

public record SubscriptionCompleted(
        UUID eventId,
        Instant occurredAt,
        UUID subscriptionId,
        UUID patientId,
        UUID dietPlanId,
        java.time.LocalDate effectiveDate,
        int cancelledPendingDeliveries,
        String correlationId) implements CorrelatedDomainEvent {

    public SubscriptionCompleted(UUID subscriptionId, UUID patientId, UUID dietPlanId, java.time.LocalDate effectiveDate,
            int cancelledPendingDeliveries, String correlationId) {
        this(UUID.randomUUID(), Instant.now(), subscriptionId, patientId, dietPlanId, effectiveDate,
                cancelledPendingDeliveries, correlationId);
    }
}
