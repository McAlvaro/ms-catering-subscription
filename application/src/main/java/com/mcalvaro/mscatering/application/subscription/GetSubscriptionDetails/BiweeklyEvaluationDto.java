package com.mcalvaro.mscatering.application.subscription.GetSubscriptionDetails;

import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO for a biweekly nutritional evaluation appointment.
 */
public record BiweeklyEvaluationDto(
        UUID id,
        LocalDate scheduledDate,
        String status,
        LocalDate completedAt) {
}
