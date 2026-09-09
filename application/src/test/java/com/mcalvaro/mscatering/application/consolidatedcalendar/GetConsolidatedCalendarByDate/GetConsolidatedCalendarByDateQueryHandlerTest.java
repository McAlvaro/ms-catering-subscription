package com.mcalvaro.mscatering.application.consolidatedcalendar.GetConsolidatedCalendarByDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetConsolidatedCalendarByDateQueryHandlerTest {

    @Mock
    private IConsolidatedCalendarQueryService queryService;

    @InjectMocks
    private GetConsolidatedCalendarByDateQueryHandler handler;

    @Test
    @DisplayName("Should return the exact consolidated calendar when the query service finds it")
    void shouldReturnExactConsolidatedCalendarWhenQueryServiceFindsIt() {
        // Arrange
        LocalDate date = LocalDate.of(2099, 1, 1);
        GetConsolidatedCalendarByDateQuery query = new GetConsolidatedCalendarByDateQuery(date);
        ConsolidatedCalendarDto expected = new ConsolidatedCalendarDto(UUID.randomUUID(), date, "CLOSED", 2, Instant.parse("2099-01-01T12:00:00Z"), "operations", List.of());
        when(queryService.getByDate(date)).thenReturn(Optional.of(expected));

        // Act
        ConsolidatedCalendarDto result = handler.handle(query);

        // Assert
        assertThat(result).isSameAs(expected).isEqualTo(expected);
        verify(queryService, times(1)).getByDate(date);
    }

    @Test
    @DisplayName("Should throw an exception when the query service does not find the consolidated calendar")
    void shouldThrowExceptionWhenQueryServiceDoesNotFindConsolidatedCalendar() {
        // Arrange
        LocalDate date = LocalDate.of(2099, 1, 1);
        GetConsolidatedCalendarByDateQuery query = new GetConsolidatedCalendarByDateQuery(date);
        when(queryService.getByDate(date)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> handler.handle(query)).isInstanceOf(IllegalArgumentException.class).hasMessage("Consolidated calendar not found for date: " + date);
        verify(queryService, times(1)).getByDate(date);
    }
}
