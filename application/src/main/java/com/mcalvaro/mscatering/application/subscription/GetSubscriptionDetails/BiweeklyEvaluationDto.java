package com.mcalvaro.mscatering.application.subscription.GetSubscriptionDetails;

import java.time.LocalDate;

/**
 * DTO for a biweekly nutritional evaluation appointment.
 */
public record BiweeklyEvaluationDto(
        String id,
        LocalDate scheduledDate,
        String status,
        LocalDate completedAt) {
}
