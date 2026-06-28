package com.mcalvaro.consolidatedcalendar.event;

import com.mcalvaro.core.DomainEvent;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ConsolidatedCalendarClosed(
        UUID eventId,
        Instant occurredAt,
        UUID calendarId,
        LocalDate date,
        int totalDeliveries) implements DomainEvent {

    public ConsolidatedCalendarClosed(UUID calendarId, LocalDate date, int totalDeliveries) {
        this(UUID.randomUUID(), Instant.now(), calendarId, date, totalDeliveries);
    }
}
