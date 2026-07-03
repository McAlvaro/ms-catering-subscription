package com.mcalvaro.mscatering.application.subscription.ReactivateSubscription;

import an.awesome.pipelinr.Command;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Command to reactivate a paused subscription before the pause period ends.
 * <p>
 * Maps to use case CU-07: Reactivar Suscripción Pausada.
 */
public record ReactivateSubscriptionCommand(
        UUID subscriptionId,
        LocalDate reactivationDate) implements Command<Void> {
}
