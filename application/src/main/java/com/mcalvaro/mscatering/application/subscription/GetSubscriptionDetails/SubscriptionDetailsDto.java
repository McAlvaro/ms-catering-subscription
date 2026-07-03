package com.mcalvaro.mscatering.application.subscription.GetSubscriptionDetails;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * DTO containing the full details of a subscription for read purposes (CQRS
 * Query side).
 * Avoids loading the heavy Aggregate Root.
 */
public record SubscriptionDetailsDto(
        UUID id,
        UUID patientId,
        UUID dietPlanId,
        String contractCode,
        String status,
        LocalDate startDate,
        LocalDate endDate,
        String serviceType,
        BigDecimal totalPrice,
        List<DeliveryDayDto> deliveryDays,
        List<BiweeklyEvaluationDto> evaluations) {
}
