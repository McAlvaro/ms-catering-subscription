package com.mcalvaro.mscatering.application.subscription.MarkNoDelivery;

import an.awesome.pipelinr.Command;
import com.mcalvaro.mscatering.domain.core.id.DeliveryDayId;
import com.mcalvaro.mscatering.domain.core.id.SubscriptionId;

/**
 * Command to mark a delivery day as NO_DELIVERY (the patient won't receive
 * food).
 * <p>
 * Maps to use case CU-04: Marcar Día como No Entrega.
 * The aggregate enforces that at least one active day remains (INV-05).
 */
public record MarkNoDeliveryCommand(
        SubscriptionId subscriptionId,
        DeliveryDayId deliveryDayId) implements Command<Void> {

    public MarkNoDeliveryCommand(String subscriptionId, String deliveryDayId) {
        this(SubscriptionId.of(subscriptionId), DeliveryDayId.of(deliveryDayId));
    }
}
