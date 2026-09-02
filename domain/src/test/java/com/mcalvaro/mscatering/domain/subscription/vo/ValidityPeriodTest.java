package com.mcalvaro.mscatering.domain.subscription.vo;

import com.mcalvaro.mscatering.domain.core.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValidityPeriodTest {

    @Test
    @DisplayName("Should create validity period successfully when dates are valid")
    void shouldCreateValidityPeriodSuccessfullyWhenDatesAreValid() {
        // Arrange
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = start.plusDays(30);
        ValidityPeriod expected = new ValidityPeriod(start, end);

        // Act
        ValidityPeriod period = new ValidityPeriod(start, end);

        // Assert
        assertThat(period.startDate()).isEqualTo(start);
        assertThat(period.endDate()).isEqualTo(end);
        assertThat(period.durationDays()).isEqualTo(31);
        assertThat(period.contains(start.plusDays(15))).isTrue();
        assertThat(period).isEqualTo(expected);
    }

    @Test
    @DisplayName("Should throw DomainException when start date is not strictly before end date")
    void shouldThrowDomainExceptionWhenStartDateIsNotBeforeEndDate() {
        // Arrange
        LocalDate date = LocalDate.of(2026, 1, 1);

        // Act & Assert
        assertThatThrownBy(() -> new ValidityPeriod(date, date))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("code", "VO-008")
                .hasMessageContaining("startDate must be strictly before endDate");
    }

    @ParameterizedTest
    @MethodSource("periodsWithNullDate")
    @DisplayName("Should throw DomainException VO-007 when either date is null")
    void shouldThrowDomainExceptionWhenEitherDateIsNull(LocalDate start, LocalDate end) {
        // Arrange
        // Dates are provided by the method source.

        // Act & Assert
        assertThatThrownBy(() -> new ValidityPeriod(start, end))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("code", "VO-007")
                .hasMessageContaining("must not be null");
    }

    @Test
    @DisplayName("Should report containment inclusively when dates are inside or outside the period")
    void shouldReportContainmentInclusivelyWhenDatesAreInsideOrOutsidePeriod() {
        // Arrange
        LocalDate start = LocalDate.of(2026, 1, 10);
        LocalDate end = LocalDate.of(2026, 1, 12);
        ValidityPeriod period = new ValidityPeriod(start, end);

        // Act & Assert
        assertThat(period.contains(start)).isTrue();
        assertThat(period.contains(end)).isTrue();
        assertThat(period.contains(start.minusDays(1))).isFalse();
        assertThat(period.contains(end.plusDays(1))).isFalse();
    }

    @Test
    @DisplayName("Should return every date including both boundaries when listing all days")
    void shouldReturnEveryDateIncludingBoundariesWhenListingAllDays() {
        // Arrange
        LocalDate start = LocalDate.of(2026, 1, 10);
        LocalDate end = LocalDate.of(2026, 1, 12);
        ValidityPeriod period = new ValidityPeriod(start, end);

        // Act
        List<LocalDate> days = period.allDays();

        // Assert
        assertThat(days).containsExactly(start, start.plusDays(1), end);
    }

    private static Stream<LocalDate[]> periodsWithNullDate() {
        LocalDate date = LocalDate.of(2026, 1, 1);
        return Stream.of(
                new LocalDate[] { null, date },
                new LocalDate[] { date, null },
                new LocalDate[] { null, null });
    }
}
