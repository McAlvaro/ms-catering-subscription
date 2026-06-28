package com.mcalvaro.consolidatedcalendar;

import com.mcalvaro.core.DomainException;

/**
 * Catalog of domain exceptions for the ConsolidatedCalendar aggregate.
 */
public final class ConsolidatedCalendarErrors {

    private ConsolidatedCalendarErrors() {
    }

    public static DomainException calendarAlreadyClosed() {
        return new DomainException("CAL-001", "Consolidated calendar is already closed.");
    }

    public static DomainException calendarHasNoLines() {
        return new DomainException("CAL-002", "Cannot close a calendar with no delivery lines.");
    }
}
