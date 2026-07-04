package com.mcalvaro.mscatering.infrastructure.api.subscription.dto;

import java.time.LocalTime;

public record ModifyDeliveryDayRequest(
        String street,
        String number,
        String city,
        String reference,
        double latitude,
        double longitude,
        String phone,
        LocalTime startTime,
        LocalTime endTime,
        String instructions
) {
}
