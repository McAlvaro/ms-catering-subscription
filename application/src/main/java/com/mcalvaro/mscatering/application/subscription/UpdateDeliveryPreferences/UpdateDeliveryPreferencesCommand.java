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
        String street,
        String number,
        String city,
        String reference,
        double latitude,
        double longitude,
        String phone,
        LocalTime startTime,
        LocalTime endTime,
        String specialInstructions) implements Command<Void> {
}
