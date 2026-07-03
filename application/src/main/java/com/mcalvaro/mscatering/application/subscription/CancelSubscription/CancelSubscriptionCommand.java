package com.mcalvaro.mscatering.application.subscription.CancelSubscription;

import an.awesome.pipelinr.Command;

import java.util.UUID;

/**
 * Command to cancel a subscription and all its pending deliveries and
 * evaluations.
 * <p>
 * Maps to use case CU-06: Cancelar Suscripción.
 */
public record CancelSubscriptionCommand(
        UUID subscriptionId,
        String reason) implements Command<Void> {
}
