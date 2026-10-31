package com.mcalvaro.mscatering.domain.consolidatedcalendar;

import com.mcalvaro.mscatering.domain.core.id.ConsolidatedCalendarId;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Domain port (repository interface) for the {@link ConsolidatedCalendar}
 * aggregate.
 * Implemented by the infrastructure layer.
 */
public interface IConsolidatedCalendarRepository {

    Optional<ConsolidatedCalendar> findById(ConsolidatedCalendarId id);

    Optional<ConsolidatedCalendar> findByDate(LocalDate date);

    void save(ConsolidatedCalendar calendar);
}
