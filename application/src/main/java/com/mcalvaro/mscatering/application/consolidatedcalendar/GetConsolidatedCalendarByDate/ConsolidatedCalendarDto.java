package com.mcalvaro.mscatering.application.consolidatedcalendar.GetConsolidatedCalendarByDate;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * DTO containing the full details of a consolidated calendar for read purposes.
 */
public record ConsolidatedCalendarDto(
        UUID id,
        LocalDate date,
        String status,
        int totalDeliveries,
        Instant closedAt,
        String closedBy,
        List<ConsolidatedLineDto> lines
) {}
