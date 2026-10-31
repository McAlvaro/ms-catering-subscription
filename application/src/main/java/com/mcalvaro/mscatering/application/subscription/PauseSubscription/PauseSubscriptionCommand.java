package com.mcalvaro.mscatering.application.subscription.PauseSubscription;

import an.awesome.pipelinr.Command;
import com.mcalvaro.mscatering.domain.core.id.SubscriptionId;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Command to pause an active subscription for a given date range.
 * <p>
 * Maps to use case CU-05: Solicitar Pausa de Suscripción.
 */
public record PauseSubscriptionCommand(
        SubscriptionId subscriptionId,
        LocalDate startDate,
        LocalDate endDate,
        String reason,
        String correlationId) implements Command<Void> {

    public PauseSubscriptionCommand(SubscriptionId subscriptionId, LocalDate startDate, LocalDate endDate, String reason) {
        this(subscriptionId, startDate, endDate, reason, UUID.randomUUID().toString());
    }

    public PauseSubscriptionCommand(String subscriptionId, LocalDate startDate, LocalDate endDate, String reason) {
        this(SubscriptionId.of(subscriptionId), startDate, endDate, reason, UUID.randomUUID().toString());
    }

    public PauseSubscriptionCommand(String subscriptionId, LocalDate startDate, LocalDate endDate, String reason, String correlationId) {
        this(SubscriptionId.of(subscriptionId), startDate, endDate, reason, correlationId);
    }
}
