package com.mcalvaro.mscatering.domain.consolidatedcalendar;

import com.mcalvaro.mscatering.domain.consolidatedcalendar.entity.ConsolidatedLine;
import com.mcalvaro.mscatering.domain.consolidatedcalendar.enums.ConsolidateStatus;
import com.mcalvaro.mscatering.domain.consolidatedcalendar.event.ConsolidatedCalendarClosed;
import com.mcalvaro.mscatering.domain.consolidatedcalendar.event.ConsolidatedCalendarCreated;
import com.mcalvaro.mscatering.domain.core.DomainException;
import com.mcalvaro.mscatering.domain.subscription.enums.ServiceType;
import com.mcalvaro.mscatering.domain.subscription.vo.DeliveryAddress;
import com.mcalvaro.mscatering.domain.subscription.vo.TimeWindow;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConsolidatedCalendarTest {

    @Test
    @DisplayName("Should create an open calendar and emit its created event")
    void shouldCreateOpenCalendarAndEmitCreatedEventWhenValidDetailsAreProvided() {
        // Arrange
        UUID calendarId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 9, 3);

        // Act
        ConsolidatedCalendar calendar = ConsolidatedCalendar.create(calendarId, date);

        // Assert
        assertThat(calendar.getId()).isEqualTo(calendarId);
        assertThat(calendar.getDate()).isEqualTo(date);
        assertThat(calendar.getStatus()).isEqualTo(ConsolidateStatus.OPEN);
        assertThat(calendar.getTotalDeliveries()).isZero();
        assertThat(calendar.getLines()).isEmpty();
        assertThat(calendar.getDomainEvents()).singleElement()
                .isInstanceOfSatisfying(ConsolidatedCalendarCreated.class, event -> {
                    assertThat(event.calendarId()).isEqualTo(calendarId);
                    assertThat(event.date()).isEqualTo(date);
                });
    }

    @Test
    @DisplayName("Should add a line and increase the delivery counter")
    void shouldAddLineAndIncreaseDeliveryCounterWhenCalendarIsOpen() {
        // Arrange
        ConsolidatedCalendar calendar = newCalendar();
        ConsolidatedLine line = newLine(calendar.getId());

        // Act
        calendar.addLine(line);

        // Assert
        assertThat(calendar.getLines()).containsExactly(line);
        assertThat(calendar.getTotalDeliveries()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should expose an unmodifiable lines list")
    void shouldExposeUnmodifiableLinesWhenLinesAreRequested() {
        // Arrange
        ConsolidatedCalendar calendar = newCalendar();
        ConsolidatedLine existingLine = newLine(calendar.getId());
        calendar.addLine(existingLine);

        // Act & Assert
        assertThatThrownBy(() -> calendar.getLines().add(newLine(calendar.getId())))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThat(calendar.getLines()).containsExactly(existingLine);
        assertThat(calendar.getTotalDeliveries()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should close a calendar and emit its closed event")
    void shouldCloseCalendarAndEmitClosedEventWhenItHasLines() {
        // Arrange
        UUID calendarId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 9, 3);
        ConsolidatedCalendar calendar = ConsolidatedCalendar.create(calendarId, date);
        calendar.addLine(newLine(calendarId));
        String closedBy = "scheduler";
        Instant beforeClosing = Instant.now();

        // Act
        calendar.close(closedBy);
        Instant afterClosing = Instant.now();

        // Assert
        assertThat(calendar.getStatus()).isEqualTo(ConsolidateStatus.CLOSED);
        assertThat(calendar.getClosedBy()).isEqualTo(closedBy);
        assertThat(calendar.getClosedAt()).isBetween(beforeClosing, afterClosing);
        assertThat(calendar.getDomainEvents()).hasSize(2);
        assertThat(calendar.getDomainEvents().get(1))
                .isInstanceOfSatisfying(ConsolidatedCalendarClosed.class, event -> {
                    assertThat(event.calendarId()).isEqualTo(calendarId);
                    assertThat(event.date()).isEqualTo(date);
                    assertThat(event.totalDeliveries()).isEqualTo(1);
                });
    }

    @Test
    @DisplayName("Should throw DomainException CAL-002 when closing a calendar with no lines")
    void shouldThrowDomainExceptionWhenClosingCalendarWithoutLines() {
        // Arrange
        ConsolidatedCalendar calendar = newCalendar();

        // Act & Assert
        assertThatThrownBy(() -> calendar.close("scheduler"))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("code", "CAL-002");
        assertThat(calendar.getStatus()).isEqualTo(ConsolidateStatus.OPEN);
        assertThat(calendar.getClosedAt()).isNull();
        assertThat(calendar.getClosedBy()).isNull();
    }

    @Test
    @DisplayName("Should throw DomainException CAL-001 and preserve state when adding a line after closure")
    void shouldThrowDomainExceptionAndPreserveStateWhenAddingLineAfterCalendarIsClosed() {
        // Arrange
        ConsolidatedCalendar calendar = closedCalendar();
        List<ConsolidatedLine> linesBeforeAttempt = calendar.getLines();
        int deliveriesBeforeAttempt = calendar.getTotalDeliveries();
        String closedByBeforeAttempt = calendar.getClosedBy();
        var closedAtBeforeAttempt = calendar.getClosedAt();
        int eventsBeforeAttempt = calendar.getDomainEvents().size();

        // Act & Assert
        assertThatThrownBy(() -> calendar.addLine(newLine(calendar.getId())))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("code", "CAL-001");
        assertThat(calendar.getStatus()).isEqualTo(ConsolidateStatus.CLOSED);
        assertThat(calendar.getLines()).containsExactlyElementsOf(linesBeforeAttempt);
        assertThat(calendar.getTotalDeliveries()).isEqualTo(deliveriesBeforeAttempt);
        assertThat(calendar.getClosedBy()).isEqualTo(closedByBeforeAttempt);
        assertThat(calendar.getClosedAt()).isEqualTo(closedAtBeforeAttempt);
        assertThat(calendar.getDomainEvents()).hasSize(eventsBeforeAttempt);
    }

    @Test
    @DisplayName("Should throw DomainException CAL-001 and preserve state when closing an already closed calendar")
    void shouldThrowDomainExceptionAndPreserveStateWhenClosingAnAlreadyClosedCalendar() {
        // Arrange
        ConsolidatedCalendar calendar = closedCalendar();
        String closedByBeforeAttempt = calendar.getClosedBy();
        var closedAtBeforeAttempt = calendar.getClosedAt();
        List<ConsolidatedLine> linesBeforeAttempt = calendar.getLines();
        int deliveriesBeforeAttempt = calendar.getTotalDeliveries();
        int eventsBeforeAttempt = calendar.getDomainEvents().size();

        // Act & Assert
        assertThatThrownBy(() -> calendar.close("another-scheduler"))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("code", "CAL-001");
        assertThat(calendar.getStatus()).isEqualTo(ConsolidateStatus.CLOSED);
        assertThat(calendar.getClosedBy()).isEqualTo(closedByBeforeAttempt);
        assertThat(calendar.getClosedAt()).isEqualTo(closedAtBeforeAttempt);
        assertThat(calendar.getLines()).containsExactlyElementsOf(linesBeforeAttempt);
        assertThat(calendar.getTotalDeliveries()).isEqualTo(deliveriesBeforeAttempt);
        assertThat(calendar.getDomainEvents()).hasSize(eventsBeforeAttempt);
    }

    private ConsolidatedCalendar newCalendar() {
        return ConsolidatedCalendar.create(UUID.randomUUID(), LocalDate.of(2026, 9, 3));
    }

    private ConsolidatedCalendar closedCalendar() {
        ConsolidatedCalendar calendar = newCalendar();
        calendar.addLine(newLine(calendar.getId()));
        calendar.close("scheduler");
        return calendar;
    }

    private ConsolidatedLine newLine(UUID calendarId) {
        return new ConsolidatedLine(UUID.randomUUID(), calendarId, UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(),
                ServiceType.LUNCH,
                new DeliveryAddress("Main Street", "10", "Madrid", "Door A", 40.4168, -3.7038, "600000000"),
                new TimeWindow(LocalTime.of(12, 0), LocalTime.of(14, 0)), "Leave at reception");
    }
}
