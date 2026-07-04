package com.mcalvaro.mscatering.infrastructure.api.subscription.dto;

import java.time.LocalTime;

public record UpdateDeliveryPreferencesRequest(
        String street,
        String number,
        String city,
        String reference,
        double latitude,
        double longitude,
        String phone,
        LocalTime startTime,
        LocalTime endTime,
        String specialInstructions
) {
}
