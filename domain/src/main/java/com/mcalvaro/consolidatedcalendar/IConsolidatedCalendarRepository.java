package com.mcalvaro.consolidatedcalendar;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * Domain port (repository interface) for the {@link ConsolidatedCalendar}
 * aggregate.
 * Implemented by the infrastructure layer.
 */
public interface IConsolidatedCalendarRepository {

    Optional<ConsolidatedCalendar> findById(UUID id);

    Optional<ConsolidatedCalendar> findByDate(LocalDate date);

    void save(ConsolidatedCalendar calendar);
}
