package com.mcalvaro.mscatering.domain.subscription.event;

import com.mcalvaro.mscatering.domain.core.DomainEvent;
import com.mcalvaro.mscatering.domain.core.id.EvaluationId;
import com.mcalvaro.mscatering.domain.core.id.SubscriptionId;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Event emitted when a bi-weekly evaluation is completed.
 * It is critical for the Nutritional Bounded Context to react to this.
 */
public record BiweeklyEvaluationCompleted(
        UUID eventId,
        Instant occurredAt,
        SubscriptionId subscriptionId,
        EvaluationId evaluationId,
        LocalDate completedAt) implements DomainEvent {

    public BiweeklyEvaluationCompleted(SubscriptionId subscriptionId, EvaluationId evaluationId, LocalDate completedAt) {
        this(UUID.randomUUID(), Instant.now(), subscriptionId, evaluationId, completedAt);
    }
}
