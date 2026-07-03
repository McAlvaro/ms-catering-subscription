package com.mcalvaro.mscatering.application.subscription.MarkNoDelivery;

import an.awesome.pipelinr.Command;

import java.util.UUID;

/**
 * Command to mark a delivery day as NO_DELIVERY (the patient won't receive
 * food).
 * <p>
 * Maps to use case CU-04: Marcar Día como No Entrega.
 * The aggregate enforces that at least one active day remains (INV-05).
 */
public record MarkNoDeliveryCommand(
        UUID subscriptionId,
        UUID deliveryDayId) implements Command<Void> {
}
