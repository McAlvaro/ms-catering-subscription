package com.mcalvaro.mscatering.application.subscription.ModifyDeliveryDay;

import an.awesome.pipelinr.Command;

import com.mcalvaro.mscatering.domain.subscription.vo.DeliveryAddress;
import com.mcalvaro.mscatering.domain.subscription.vo.TimeWindow;

import java.util.UUID;

/**
 * Command to modify the delivery details of a specific day.
 * <p>
 * Maps to use case CU-03: Cambiar Dirección de Entrega de un Día.
 * The 48-hour anticipation rule (RN-02) is enforced by the aggregate.
 */
public record ModifyDeliveryDayCommand(
        UUID subscriptionId,
        UUID deliveryDayId,
        DeliveryAddress newAddress,
        TimeWindow newTimeWindow,
        String newInstructions) implements Command<Void> {
}
