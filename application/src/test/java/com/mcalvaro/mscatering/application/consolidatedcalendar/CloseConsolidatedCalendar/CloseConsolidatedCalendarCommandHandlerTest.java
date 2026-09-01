package com.mcalvaro.mscatering.application.consolidatedcalendar.CloseConsolidatedCalendar;

import com.mcalvaro.mscatering.domain.consolidatedcalendar.ConsolidatedCalendar;
import com.mcalvaro.mscatering.domain.consolidatedcalendar.IConsolidatedCalendarRepository;
import com.mcalvaro.mscatering.domain.consolidatedcalendar.entity.ConsolidatedLine;
import com.mcalvaro.mscatering.domain.consolidatedcalendar.service.DailyConsolidator;
import com.mcalvaro.mscatering.domain.core.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link CloseConsolidatedCalendarCommandHandler}.
 * <p>
 * Strategy: mock both infrastructure collaborators (DailyConsolidator and
 * IConsolidatedCalendarRepository) so the test validates ONLY the orchestration
 * logic of the handler — no database, no Spring context needed.
 */
@ExtendWith(MockitoExtension.class)
class CloseConsolidatedCalendarCommandHandlerTest {

    @Mock
    private DailyConsolidator dailyConsolidator;

    @Mock
    private IConsolidatedCalendarRepository calendarRepository;

    @InjectMocks
    private CloseConsolidatedCalendarCommandHandler handler;

    @Test
    @DisplayName("Should close calendar and return its ID when consolidation succeeds")
    void shouldCloseCalendarAndReturnItsIdWhenConsolidationSucceeds() {
        // Arrange
        LocalDate targetDate = LocalDate.of(2025, 6, 15);
        String closedBy = "supervisor@catering.com";
        CloseConsolidatedCalendarCommand command = new CloseConsolidatedCalendarCommand(targetDate, closedBy);

        // Prepare a real ConsolidatedCalendar with one line so close() doesn't throw
        ConsolidatedCalendar calendar = buildCalendarWithOneLine(targetDate);

        // Stub: when the domain service is called for that date, return our prepared
        // calendar
        when(dailyConsolidator.consolidateForDate(targetDate)).thenReturn(calendar);

        // Act
        UUID returnedId = handler.handle(command);

        // Assert – the handler must return the calendar's own ID
        assertThat(returnedId).isEqualTo(calendar.getId());

        verify(calendarRepository, times(1)).save(calendar);
    }

    @Test
    @DisplayName("Should invoke DailyConsolidator with the exact date from the command")
    void shouldInvokeDailyConsolidatorWithExactCommandDate() {
        // Arrange
        LocalDate targetDate = LocalDate.of(2025, 7, 1);
        LocalDate wrongDate = targetDate.plusDays(1);
        CloseConsolidatedCalendarCommand command = new CloseConsolidatedCalendarCommand(targetDate, "ops-team");

        ConsolidatedCalendar calendar = buildCalendarWithOneLine(targetDate);
        when(dailyConsolidator.consolidateForDate(targetDate)).thenReturn(calendar);

        // Act
        handler.handle(command);

        verify(dailyConsolidator, times(1)).consolidateForDate(targetDate);
        verify(dailyConsolidator, never()).consolidateForDate(wrongDate);
    }

    @Test
    @DisplayName("Should propagate DomainException when calendar has no delivery lines")
    void shouldPropagateDomainExceptionWhenCalendarHasNoLines() {
        // Arrange
        LocalDate targetDate = LocalDate.of(2025, 8, 20);
        CloseConsolidatedCalendarCommand command = new CloseConsolidatedCalendarCommand(targetDate, "admin");

        // Return an EMPTY calendar (no lines added) — close() will throw CAL-002
        ConsolidatedCalendar emptyCalendar = ConsolidatedCalendar.create(UUID.randomUUID(), targetDate);
        when(dailyConsolidator.consolidateForDate(targetDate)).thenReturn(emptyCalendar);

        // Act & Assert
        assertThatThrownBy(() -> handler.handle(command))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("code", "CAL-002")
                .hasMessageContaining("no delivery lines");

        // Verify – the repository must NEVER be called if an exception occurs
        verify(calendarRepository, never()).save(any());
    }

    /**
     * Builds a {@link ConsolidatedCalendar} with one pre-added delivery line
     * so that {@code calendar.close()} can succeed without throwing
     * {@code CAL-002}.
     * <p>
     * Using a real aggregate (not a mock) is intentional: we want to test that
     * the handler correctly triggers the state transition inside a real domain
     * object.
     */
    private ConsolidatedCalendar buildCalendarWithOneLine(LocalDate date) {
        ConsolidatedCalendar calendar = ConsolidatedCalendar.create(UUID.randomUUID(), date);

        ConsolidatedLine line = mock(ConsolidatedLine.class);
        calendar.addLine(line);

        return calendar;
    }
}
