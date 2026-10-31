package com.mcalvaro.mscatering.application.subscription.CancelSubscription;

import an.awesome.pipelinr.Command;
import com.mcalvaro.mscatering.domain.core.id.SubscriptionId;

import java.util.UUID;

/**
 * Command to cancel a subscription and all its pending deliveries and
 * evaluations.
 * <p>
 * Maps to use case CU-06: Cancelar Suscripción.
 */
public record CancelSubscriptionCommand(
        SubscriptionId subscriptionId,
        String reason,
        String correlationId) implements Command<Void> {

    public CancelSubscriptionCommand(SubscriptionId subscriptionId, String reason) {
        this(subscriptionId, reason, UUID.randomUUID().toString());
    }

    public CancelSubscriptionCommand(String subscriptionId, String reason) {
        this(SubscriptionId.of(subscriptionId), reason, UUID.randomUUID().toString());
    }

    public CancelSubscriptionCommand(String subscriptionId, String reason, String correlationId) {
        this(SubscriptionId.of(subscriptionId), reason, correlationId);
    }
}
