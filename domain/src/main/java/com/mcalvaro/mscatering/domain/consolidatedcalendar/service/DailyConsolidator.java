package com.mcalvaro.mscatering.domain.consolidatedcalendar.service;

import com.mcalvaro.mscatering.domain.consolidatedcalendar.ConsolidatedCalendar;

import java.time.LocalDate;

/**
 * Domain Service for consolidating the daily catering deliveries.
 * <p>
 * This process crosses the boundaries of multiple active subscriptions to
 * gather
 * all scheduled delivery days for a specific date and orchestrate the creation
 * of the {@link ConsolidatedCalendar}.
 */
public interface DailyConsolidator {

    /**
     * Gathers all scheduled deliveries for the specified date across all active
     * subscriptions and creates a closed {@link ConsolidatedCalendar}.
     *
     * @param deliveryDate The target delivery date to consolidate.
     * @return The newly created consolidated calendar.
     */
    ConsolidatedCalendar consolidateForDate(LocalDate deliveryDate);
}
