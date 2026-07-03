package com.mcalvaro.mscatering.application.consolidatedcalendar.GetConsolidatedCalendarByDate;

import java.time.LocalTime;
import java.util.UUID;

/**
 * DTO for a single consolidated delivery line (production/logistics view).
 */
public record ConsolidatedLineDto(
        UUID id,
        UUID subscriptionId,
        UUID patientId,
        UUID dietPlanId,
        String serviceType,
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
}
