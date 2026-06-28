package com.mcalvaro.consolidatedcalendar;

import com.mcalvaro.consolidatedcalendar.entity.ConsolidatedLine;
import com.mcalvaro.consolidatedcalendar.enums.ConsolidateStatus;
import com.mcalvaro.consolidatedcalendar.event.ConsolidatedCalendarClosed;
import com.mcalvaro.consolidatedcalendar.event.ConsolidatedCalendarCreated;
import com.mcalvaro.core.AggregateRoot;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Aggregate Root — ConsolidatedCalendar (CalendarioConsolidado).
 * <p>
 * Immutable daily grouping of all active deliveries. Once closed,
 * it triggers production (BC4) and logistics (BC5) via domain events.
 */
public class ConsolidatedCalendar extends AggregateRoot {

    private final LocalDate date;
    private ConsolidateStatus status;
    private int totalDeliveries;
    private Instant closedAt;
    private String closedBy;
    private final List<ConsolidatedLine> lines = new ArrayList<>();

    private ConsolidatedCalendar(UUID id, LocalDate date) {
        super(id);
        this.date = date;
        this.status = ConsolidateStatus.OPEN;
        this.totalDeliveries = 0;
    }

    /**
     * Creates a new open consolidated calendar for the given date.
     * Emits {@link ConsolidatedCalendarCreated}.
     */
    public static ConsolidatedCalendar create(UUID id, LocalDate date) {
        ConsolidatedCalendar calendar = new ConsolidatedCalendar(id, date);
        calendar.addDomainEvent(new ConsolidatedCalendarCreated(id, date));
        return calendar;
    }

    /**
     * Adds a delivery line. Only allowed while the calendar is {@code OPEN}.
     */
    public void addLine(ConsolidatedLine line) {
        if (status != ConsolidateStatus.OPEN) {
            throw ConsolidatedCalendarErrors.calendarAlreadyClosed();
        }
        lines.add(line);
        totalDeliveries++;
    }

    /**
     * Closes the calendar. Requires at least one delivery line.
     * Emits {@link ConsolidatedCalendarClosed}.
     */
    public void close(String closedBy) {
        if (status != ConsolidateStatus.OPEN) {
            throw ConsolidatedCalendarErrors.calendarAlreadyClosed();
        }
        if (lines.isEmpty()) {
            throw ConsolidatedCalendarErrors.calendarHasNoLines();
        }
        this.status = ConsolidateStatus.CLOSED;
        this.closedAt = Instant.now();
        this.closedBy = closedBy;
        addDomainEvent(new ConsolidatedCalendarClosed(getId(), date, totalDeliveries));
    }

    public List<ConsolidatedLine> getLines() {
        return Collections.unmodifiableList(lines);
    }

    public LocalDate getDate() {
        return date;
    }

    public ConsolidateStatus getStatus() {
        return status;
    }

    public int getTotalDeliveries() {
        return totalDeliveries;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public String getClosedBy() {
        return closedBy;
    }
}
