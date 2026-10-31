package com.mcalvaro.mscatering.application.consolidatedcalendar.CloseConsolidatedCalendar;

import an.awesome.pipelinr.Command;
import com.mcalvaro.mscatering.domain.core.id.ConsolidatedCalendarId;

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
        String closedBy,
        String correlationId) implements Command<ConsolidatedCalendarId> {
    public CloseConsolidatedCalendarCommand(LocalDate date, String closedBy) {
        this(date, closedBy, UUID.randomUUID().toString());
    }
}
