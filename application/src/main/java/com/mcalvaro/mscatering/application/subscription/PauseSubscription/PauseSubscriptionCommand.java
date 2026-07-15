package com.mcalvaro.mscatering.application.subscription.PauseSubscription;

import an.awesome.pipelinr.Command;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Command to pause an active subscription for a given date range.
 * <p>
 * Maps to use case CU-05: Solicitar Pausa de Suscripción.
 * Contains only primitive types — the Handler builds the PauseRange VO.
 */
public record PauseSubscriptionCommand(
        UUID subscriptionId,
        LocalDate startDate,
        LocalDate endDate,
        String reason) implements Command<Void> {
}
