package com.mcalvaro.mscatering.application.subscription.UpdateDeliveryPreferences;

import an.awesome.pipelinr.Command;

import java.time.LocalTime;
import java.util.UUID;

/**
 * Command to update the default delivery preferences of a subscription.
 * Contains only primitive types — the Handler builds DeliveryPreferences VO.
 */
public record UpdateDeliveryPreferencesCommand(
        UUID subscriptionId,
        UUID deliveryAddressId,
        LocalTime startTime,
        LocalTime endTime,
        String specialInstructions) implements Command<Void> {
}
