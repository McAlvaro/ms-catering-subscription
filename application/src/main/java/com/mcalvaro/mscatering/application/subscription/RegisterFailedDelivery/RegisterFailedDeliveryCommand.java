package com.mcalvaro.mscatering.application.subscription.RegisterFailedDelivery;

import an.awesome.pipelinr.Command;

import java.util.UUID;

/**
 * Command to register that a delivery failed (e.g., patient not found at
 * address).
 */
public record RegisterFailedDeliveryCommand(
        UUID subscriptionId,
        UUID deliveryDayId,
        String reason) implements Command<Void> {
}
