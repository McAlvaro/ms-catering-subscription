package com.mcalvaro.mscatering.domain.subscription.event;

import com.mcalvaro.mscatering.domain.core.DomainEvent;

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
        UUID subscriptionId,
        UUID evaluationId,
        LocalDate completedAt) implements DomainEvent {

    public BiweeklyEvaluationCompleted(UUID subscriptionId, UUID evaluationId, LocalDate completedAt) {
        this(UUID.randomUUID(), Instant.now(), subscriptionId, evaluationId, completedAt);
    }
}
