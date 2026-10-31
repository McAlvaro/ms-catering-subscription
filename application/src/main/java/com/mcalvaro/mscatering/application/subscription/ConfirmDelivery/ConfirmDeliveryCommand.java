package com.mcalvaro.mscatering.application.subscription.ConfirmDelivery;

import an.awesome.pipelinr.Command;
import com.mcalvaro.mscatering.domain.core.id.DeliveryDayId;
import com.mcalvaro.mscatering.domain.core.id.SubscriptionId;

/**
 * Command to confirm that a delivery was successfully completed.
 * Usually triggered by the Logistics Bounded Context (BC5).
 */
public record ConfirmDeliveryCommand(
        SubscriptionId subscriptionId,
        DeliveryDayId deliveryDayId) implements Command<Void> {

    public ConfirmDeliveryCommand(String subscriptionId, String deliveryDayId) {
        this(SubscriptionId.of(subscriptionId), DeliveryDayId.of(deliveryDayId));
    }
}
