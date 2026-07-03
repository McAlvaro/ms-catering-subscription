package com.mcalvaro.mscatering.application.subscription.CompleteSubscription;

import an.awesome.pipelinr.Command;

import java.util.UUID;

/**
 * Command to complete a subscription whose contract period has ended.
 */
public record CompleteSubscriptionCommand(
        UUID subscriptionId) implements Command<Void> {
}
