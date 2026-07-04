package com.mcalvaro.mscatering.infrastructure.api.subscription.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record CreateSubscriptionRequest(
        UUID patientId,
        UUID dietPlanId,
        LocalDate startDate,
        LocalDate endDate,
        String serviceType,
        BigDecimal totalPrice,
        String acceptedConditions,
        String prefStreet,
        String prefNumber,
        String prefCity,
        String prefReference,
        double prefLatitude,
        double prefLongitude,
        String prefPhone,
        LocalTime prefTimeStart,
        LocalTime prefTimeEnd,
        String prefSpecialInstructions
) {
}
