package com.mcalvaro.mscatering.domain.subscription.vo;

import com.mcalvaro.mscatering.domain.core.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDate;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PauseRangeTest {

    @Test
    @DisplayName("Should create a pause range when it starts at least two days ahead and ends later")
    void shouldCreatePauseRangeWhenStartHasRequiredNoticeAndEndIsLater() {
        // Arrange
        LocalDate start = LocalDate.now().plusDays(2);
        LocalDate end = start.plusDays(3);

        // Act
        PauseRange range = new PauseRange(start, end);

        // Assert
        assertThat(range.startDate()).isEqualTo(start);
        assertThat(range.endDate()).isEqualTo(end);
        assertThat(range).isEqualTo(new PauseRange(start, end));
    }

    @ParameterizedTest
    @MethodSource("rangesWithNullDate")
    @DisplayName("Should throw DomainException VO-013 when either pause date is null")
    void shouldThrowDomainExceptionWhenEitherPauseDateIsNull(LocalDate start, LocalDate end) {
        // Arrange
        // Dates are provided by the method source.

        // Act & Assert
        assertThatThrownBy(() -> new PauseRange(start, end))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("code", "VO-013")
                .hasMessageContaining("must not be null");
    }

    @Test
    @DisplayName("Should throw DomainException SUB-001 when pause starts with less than two days notice")
    void shouldThrowDomainExceptionWhenPauseStartsWithLessThanTwoDaysNotice() {
        // Arrange
        LocalDate start = LocalDate.now().plusDays(1);
        LocalDate end = start.plusDays(2);

        // Act & Assert
        assertThatThrownBy(() -> new PauseRange(start, end))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("code", "SUB-001")
                .hasMessageContaining("at least 48 hours in advance");
    }

    @ParameterizedTest
    @MethodSource("rangesNotInAscendingOrder")
    @DisplayName("Should throw DomainException VO-014 when pause start is not before end")
    void shouldThrowDomainExceptionWhenPauseStartIsNotBeforeEnd(LocalDate start, LocalDate end) {
        // Arrange
        // Dates are provided by the method source.

        // Act & Assert
        assertThatThrownBy(() -> new PauseRange(start, end))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("code", "VO-014")
                .hasMessageContaining("startDate must be strictly before endDate");
    }

    private static Stream<LocalDate[]> rangesWithNullDate() {
        LocalDate validDate = LocalDate.now().plusDays(3);
        return Stream.of(
                new LocalDate[] { null, validDate },
                new LocalDate[] { validDate, null },
                new LocalDate[] { null, null });
    }

    private static Stream<LocalDate[]> rangesNotInAscendingOrder() {
        LocalDate start = LocalDate.now().plusDays(3);
        return Stream.of(
                new LocalDate[] { start, start },
                new LocalDate[] { start, start.minusDays(1) });
    }
}
