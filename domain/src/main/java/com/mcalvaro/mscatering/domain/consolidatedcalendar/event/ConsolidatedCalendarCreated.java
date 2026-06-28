package com.mcalvaro.mscatering.domain.consolidatedcalendar.event;

import com.mcalvaro.mscatering.domain.core.DomainEvent;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ConsolidatedCalendarCreated(
        UUID eventId,
        Instant occurredAt,
        UUID calendarId,
        LocalDate date) implements DomainEvent {

    public ConsolidatedCalendarCreated(UUID calendarId, LocalDate date) {
        this(UUID.randomUUID(), Instant.now(), calendarId, date);
    }
}
