package com.mcalvaro.mscatering.application.subscription.CompleteSubscription;

import an.awesome.pipelinr.Command;
import com.mcalvaro.mscatering.domain.core.id.SubscriptionId;

import java.util.UUID;

/**
 * Command to complete a subscription whose contract period has ended.
 */
public record CompleteSubscriptionCommand(
        SubscriptionId subscriptionId,
        String correlationId) implements Command<Void> {

    public CompleteSubscriptionCommand(SubscriptionId subscriptionId) {
        this(subscriptionId, UUID.randomUUID().toString());
    }

    public CompleteSubscriptionCommand(String subscriptionId) {
        this(SubscriptionId.of(subscriptionId), UUID.randomUUID().toString());
    }

    public CompleteSubscriptionCommand(String subscriptionId, String correlationId) {
        this(SubscriptionId.of(subscriptionId), correlationId);
    }
}
