package com.mcalvaro.mscatering.application.consolidatedcalendar.GetConsolidatedCalendarByDate;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Port (abstraction) for reading Consolidated Calendar data (CQRS Read side).
 * Implemented by the Infrastructure layer.
 */
public interface IConsolidatedCalendarQueryService {

    /**
     * Retrieves the consolidated calendar for a specific date.
     */
    Optional<ConsolidatedCalendarDto> getByDate(LocalDate date);
}
