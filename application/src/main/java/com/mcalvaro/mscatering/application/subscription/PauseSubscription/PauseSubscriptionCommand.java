package com.mcalvaro.mscatering.application.subscription.PauseSubscription;

import an.awesome.pipelinr.Command;

import com.mcalvaro.mscatering.domain.subscription.vo.PauseRange;

import java.util.UUID;

/**
 * Command to pause an active subscription for a given date range.
 * <p>
 * Maps to use case CU-05: Solicitar Pausa de Suscripción.
 */
public record PauseSubscriptionCommand(
        UUID subscriptionId,
        PauseRange range,
        String reason) implements Command<Void> {
}
