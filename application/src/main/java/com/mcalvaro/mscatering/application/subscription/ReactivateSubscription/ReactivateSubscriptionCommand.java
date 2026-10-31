package com.mcalvaro.mscatering.application.subscription.ReactivateSubscription;

import an.awesome.pipelinr.Command;
import com.mcalvaro.mscatering.domain.core.id.SubscriptionId;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Command to reactivate a paused subscription before the pause period ends.
 * <p>
 * Maps to use case CU-07: Reactivar Suscripción Pausada.
 */
public record ReactivateSubscriptionCommand(
        SubscriptionId subscriptionId,
        LocalDate reactivationDate,
        String correlationId) implements Command<Void> {

    public ReactivateSubscriptionCommand(SubscriptionId subscriptionId, LocalDate reactivationDate) {
        this(subscriptionId, reactivationDate, UUID.randomUUID().toString());
    }

    public ReactivateSubscriptionCommand(String subscriptionId, LocalDate reactivationDate) {
        this(SubscriptionId.of(subscriptionId), reactivationDate, UUID.randomUUID().toString());
    }

    public ReactivateSubscriptionCommand(String subscriptionId, LocalDate reactivationDate, String correlationId) {
        this(SubscriptionId.of(subscriptionId), reactivationDate, correlationId);
    }
}
