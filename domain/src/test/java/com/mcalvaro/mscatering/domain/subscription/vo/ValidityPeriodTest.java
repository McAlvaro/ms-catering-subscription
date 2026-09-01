package com.mcalvaro.mscatering.domain.subscription.vo;

import com.mcalvaro.mscatering.domain.core.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValidityPeriodTest {

    @Test
    @DisplayName("Should create validity period successfully when dates are valid")
    void shouldCreateValidityPeriodSuccessfully() {
        // Arrange
        LocalDate start = LocalDate.now();
        LocalDate end = start.plusDays(30);

        // Act
        ValidityPeriod period = new ValidityPeriod(start, end);

        // Assert
        assertThat(period.startDate()).isEqualTo(start);
        assertThat(period.endDate()).isEqualTo(end);
        assertThat(period.durationDays()).isEqualTo(31);
        assertThat(period.contains(start.plusDays(15))).isTrue();
    }

    @Test
    @DisplayName("Should throw DomainException when start date is not strictly before end date")
    void shouldThrowExceptionWhenStartDateIsNotBeforeEndDate() {
        // Arrange
        LocalDate date = LocalDate.now();

        // Act & Assert
        assertThatThrownBy(() -> new ValidityPeriod(date, date))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("code", "VO-008")
                .hasMessageContaining("startDate must be strictly before endDate");
    }

    @Test
    @DisplayName("Should throw DomainException when any date is null")
    void shouldThrowExceptionWhenDatesAreNull() {
        // Arrange
        LocalDate date = LocalDate.now();

        // Act & Assert
        assertThatThrownBy(() -> new ValidityPeriod(null, date))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("code", "VO-007");

        assertThatThrownBy(() -> new ValidityPeriod(date, null))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("code", "VO-007");
    }
}
