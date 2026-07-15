package com.mcalvaro.mscatering.application.subscription.ModifyDeliveryDay;

import an.awesome.pipelinr.Command;

import java.time.LocalTime;
import java.util.UUID;

/**
 * Command to modify the delivery details of a specific day.
 * <p>
 * Maps to use case CU-03: Cambiar Dirección de Entrega de un Día.
 * Contains only primitive types — the Handler builds DeliveryAddress and TimeWindow VOs.
 * The 48-hour anticipation rule (RN-02) is enforced by the aggregate.
 */
public record ModifyDeliveryDayCommand(
        UUID subscriptionId,
        UUID deliveryDayId,
        String street,
        String number,
        String city,
        String reference,
        double latitude,
        double longitude,
        String phone,
        LocalTime startTime,
        LocalTime endTime,
        String instructions) implements Command<Void> {
}
