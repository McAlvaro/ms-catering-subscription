package com.mcalvaro.mscatering.domain.consolidatedcalendar.event;

import com.mcalvaro.mscatering.domain.core.CorrelatedDomainEvent;
import com.mcalvaro.mscatering.domain.core.id.ConsolidatedCalendarId;
import com.mcalvaro.mscatering.domain.consolidatedcalendar.entity.ConsolidatedDeliverySnapshot;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ConsolidatedCalendarClosed(
        UUID eventId,
        Instant occurredAt,
        ConsolidatedCalendarId calendarId,
        LocalDate date,
        int totalDeliveries,
        String correlationId,
        java.util.List<ConsolidatedDeliverySnapshot> deliveries) implements CorrelatedDomainEvent {

    public ConsolidatedCalendarClosed {
        deliveries = java.util.List.copyOf(deliveries);
        if (correlationId == null || correlationId.isBlank() || totalDeliveries != deliveries.size()) {
            throw new IllegalArgumentException("Closed-calendar event snapshot is invalid");
        }
    }

    public ConsolidatedCalendarClosed(ConsolidatedCalendarId calendarId, LocalDate date, int totalDeliveries) {
        this(UUID.randomUUID(), Instant.now(), calendarId, date, totalDeliveries, UUID.randomUUID().toString(), java.util.List.of());
    }
}
