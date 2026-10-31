package com.mcalvaro.mscatering.application.subscription.ModifyDeliveryDay;

import an.awesome.pipelinr.Command;
import com.mcalvaro.mscatering.domain.core.id.DeliveryDayId;
import com.mcalvaro.mscatering.domain.core.id.SubscriptionId;

import java.time.LocalTime;

/**
 * Command to modify the delivery details of a specific day.
 * <p>
 * Maps to use case CU-03: Cambiar Dirección de Entrega de un Día.
 * Contains only primitive types — the Handler builds DeliveryAddress and TimeWindow VOs.
 * The 48-hour anticipation rule (RN-02) is enforced by the aggregate.
 */
public record ModifyDeliveryDayCommand(
        SubscriptionId subscriptionId,
        DeliveryDayId deliveryDayId,
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

    public ModifyDeliveryDayCommand(
            String subscriptionId,
            String deliveryDayId,
            String street,
            String number,
            String city,
            String reference,
            double latitude,
            double longitude,
            String phone,
            LocalTime startTime,
            LocalTime endTime,
            String instructions) {
        this(SubscriptionId.of(subscriptionId), DeliveryDayId.of(deliveryDayId),
                street, number, city, reference, latitude, longitude, phone, startTime, endTime, instructions);
    }
}
