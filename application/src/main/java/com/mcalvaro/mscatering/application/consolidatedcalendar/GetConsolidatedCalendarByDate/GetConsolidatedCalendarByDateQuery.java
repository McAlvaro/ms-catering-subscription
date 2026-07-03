package com.mcalvaro.mscatering.application.consolidatedcalendar.GetConsolidatedCalendarByDate;

import an.awesome.pipelinr.Command;

import java.time.LocalDate;

/**
 * Query to retrieve the consolidated calendar for a specific date.
 * Used by production (BC4) and logistics (BC5) to obtain the daily delivery
 * plan.
 */
public record GetConsolidatedCalendarByDateQuery(
        LocalDate date) implements Command<ConsolidatedCalendarDto> {
}
