package com.mcalvaro.mscatering.application.subscription.UpdateDeliveryPreferences;

import an.awesome.pipelinr.Command;
import com.mcalvaro.mscatering.domain.core.id.AddressId;
import com.mcalvaro.mscatering.domain.core.id.SubscriptionId;

import java.time.LocalTime;

/**
 * Command to update the default delivery preferences of a subscription.
 * Contains only primitive types — the Handler builds DeliveryPreferences VO.
 */
public record UpdateDeliveryPreferencesCommand(
        SubscriptionId subscriptionId,
        AddressId deliveryAddressId,
        LocalTime startTime,
        LocalTime endTime,
        String specialInstructions) implements Command<Void> {

    public UpdateDeliveryPreferencesCommand(
            String subscriptionId,
            String deliveryAddressId,
            LocalTime startTime,
            LocalTime endTime,
            String specialInstructions) {
        this(SubscriptionId.of(subscriptionId), AddressId.of(deliveryAddressId),
                startTime, endTime, specialInstructions);
    }
}
