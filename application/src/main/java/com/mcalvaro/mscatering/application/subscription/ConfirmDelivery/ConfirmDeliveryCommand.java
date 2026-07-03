package com.mcalvaro.mscatering.application.subscription.ConfirmDelivery;

import an.awesome.pipelinr.Command;

import java.util.UUID;

/**
 * Command to confirm that a delivery was successfully completed.
 * Usually triggered by the Logistics Bounded Context (BC5).
 */
public record ConfirmDeliveryCommand(
        UUID subscriptionId,
        UUID deliveryDayId) implements Command<Void> {
}
