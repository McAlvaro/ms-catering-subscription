package com.mcalvaro.mscatering.application.consolidatedcalendar.GetConsolidatedCalendarByDate;

import java.time.LocalTime;

/**
 * DTO for a single consolidated delivery line (production/logistics view).
 */
public record ConsolidatedLineDto(
        String id,
        String subscriptionId,
        String patientId,
        String dietPlanId,
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
