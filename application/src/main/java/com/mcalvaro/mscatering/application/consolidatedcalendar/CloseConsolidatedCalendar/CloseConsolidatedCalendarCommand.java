package com.mcalvaro.mscatering.application.consolidatedcalendar.CloseConsolidatedCalendar;

import an.awesome.pipelinr.Command;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Command to trigger the daily consolidation and closure of the calendar.
 * <p>
 * Maps to use case CU-08: Cierre Diario del Calendario Consolidado.
 * This is typically triggered by an automated scheduled process at the
 * end of each business day.
 */
public record CloseConsolidatedCalendarCommand(
        LocalDate date,
        String closedBy) implements Command<UUID> {
}
