package com.mcalvaro.mscatering.application.subscription.GetSubscriptionDetails;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * DTO for a single delivery day in the subscription calendar.
 */
public record DeliveryDayDto(
        UUID id,
        LocalDate date,
        String status,
        String street,
        String number,
        String city,
        String reference,
        double latitude,
        double longitude,
        String phone,
        LocalTime startTime,
        LocalTime endTime,
        String specialInstructions) {
}
