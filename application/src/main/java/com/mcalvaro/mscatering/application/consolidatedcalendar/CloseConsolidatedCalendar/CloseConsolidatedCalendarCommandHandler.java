package com.mcalvaro.mscatering.application.consolidatedcalendar.CloseConsolidatedCalendar;

import an.awesome.pipelinr.Command;

import com.mcalvaro.mscatering.application.abstractions.UnitOfWork;
import com.mcalvaro.mscatering.domain.consolidatedcalendar.ConsolidatedCalendar;
import com.mcalvaro.mscatering.domain.consolidatedcalendar.IConsolidatedCalendarRepository;
import com.mcalvaro.mscatering.domain.consolidatedcalendar.service.DailyConsolidator;

import java.util.UUID;

/**
 * Handler for {@link CloseConsolidatedCalendarCommand}.
 * <p>
 * Orchestrates the daily consolidation process:
 * <ol>
 * <li>The {@link DailyConsolidator} domain service gathers all scheduled
 * deliveries for the target date across all active subscriptions and
 * creates a new {@link ConsolidatedCalendar} with its lines.</li>
 * <li>The calendar is closed by the specified operator, which emits
 * {@code ConsolidatedCalendarClosed} — the event that triggers
 * production (BC4) and logistics (BC5).</li>
 * <li>The aggregate is persisted and the transaction is committed.</li>
 * </ol>
 */
public class CloseConsolidatedCalendarCommandHandler
        implements Command.Handler<CloseConsolidatedCalendarCommand, UUID> {

    private final DailyConsolidator dailyConsolidator;
    private final IConsolidatedCalendarRepository calendarRepository;
    private final UnitOfWork unitOfWork;

    public CloseConsolidatedCalendarCommandHandler(DailyConsolidator dailyConsolidator,
            IConsolidatedCalendarRepository calendarRepository,
            UnitOfWork unitOfWork) {
        this.dailyConsolidator = dailyConsolidator;
        this.calendarRepository = calendarRepository;
        this.unitOfWork = unitOfWork;
    }

    @Override
    public UUID handle(CloseConsolidatedCalendarCommand command) {
        // 1. Consolidate all scheduled deliveries for the given date
        ConsolidatedCalendar calendar = dailyConsolidator.consolidateForDate(command.date());

        // 2. Close the calendar — emits ConsolidatedCalendarClosed event
        calendar.close(command.closedBy());

        // 3. Persist and commit atomically
        calendarRepository.save(calendar);
        unitOfWork.commit();

        return calendar.getId();
    }
}
