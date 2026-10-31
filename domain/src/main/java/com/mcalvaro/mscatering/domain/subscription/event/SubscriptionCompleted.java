package com.mcalvaro.mscatering.domain.subscription.event;

import com.mcalvaro.mscatering.domain.core.CorrelatedDomainEvent;
import com.mcalvaro.mscatering.domain.core.id.DietPlanId;
import com.mcalvaro.mscatering.domain.core.id.PatientId;
import com.mcalvaro.mscatering.domain.core.id.SubscriptionId;

import java.time.Instant;
import java.util.UUID;

public record SubscriptionCompleted(
        UUID eventId,
        Instant occurredAt,
        SubscriptionId subscriptionId,
        PatientId patientId,
        DietPlanId dietPlanId,
        java.time.LocalDate effectiveDate,
        int cancelledPendingDeliveries,
        String correlationId) implements CorrelatedDomainEvent {

    public SubscriptionCompleted(SubscriptionId subscriptionId, PatientId patientId, DietPlanId dietPlanId, java.time.LocalDate effectiveDate,
            int cancelledPendingDeliveries, String correlationId) {
        this(UUID.randomUUID(), Instant.now(), subscriptionId, patientId, dietPlanId, effectiveDate,
                cancelledPendingDeliveries, correlationId);
    }
}
