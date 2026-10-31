package com.mcalvaro.mscatering.application.subscription.RegisterFailedDelivery;

import an.awesome.pipelinr.Command;
import com.mcalvaro.mscatering.domain.core.id.DeliveryDayId;
import com.mcalvaro.mscatering.domain.core.id.SubscriptionId;

/**
 * Command to register that a delivery failed (e.g., patient not found at
 * address).
 */
public record RegisterFailedDeliveryCommand(
        SubscriptionId subscriptionId,
        DeliveryDayId deliveryDayId,
        String reason) implements Command<Void> {

    public RegisterFailedDeliveryCommand(String subscriptionId, String deliveryDayId, String reason) {
        this(SubscriptionId.of(subscriptionId), DeliveryDayId.of(deliveryDayId), reason);
    }
}
