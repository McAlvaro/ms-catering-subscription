package com.mcalvaro.mscatering.application.consolidatedcalendar.GetConsolidatedCalendarByDate;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO containing the full details of a consolidated calendar for read purposes.
 */
public record ConsolidatedCalendarDto(
        String id,
        LocalDate date,
        String status,
        int totalDeliveries,
        Instant closedAt,
        String closedBy,
        List<ConsolidatedLineDto> lines
) {}
