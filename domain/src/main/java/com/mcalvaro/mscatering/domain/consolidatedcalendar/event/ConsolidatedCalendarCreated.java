package com.mcalvaro.mscatering.domain.consolidatedcalendar.event;

import com.mcalvaro.mscatering.domain.core.DomainEvent;
import com.mcalvaro.mscatering.domain.core.id.ConsolidatedCalendarId;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ConsolidatedCalendarCreated(
        UUID eventId,
        Instant occurredAt,
        ConsolidatedCalendarId calendarId,
        LocalDate date) implements DomainEvent {

    public ConsolidatedCalendarCreated(ConsolidatedCalendarId calendarId, LocalDate date) {
        this(UUID.randomUUID(), Instant.now(), calendarId, date);
    }
}
