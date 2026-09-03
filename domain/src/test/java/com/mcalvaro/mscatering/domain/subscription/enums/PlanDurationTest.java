package com.mcalvaro.mscatering.domain.subscription.enums;

import com.mcalvaro.mscatering.domain.core.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlanDurationTest {

    @ParameterizedTest
    @MethodSource("validDurations")
    @DisplayName("Should map each valid day count to its plan duration")
    void shouldMapPlanDurationWhenDayCountIsValid(int days, PlanDuration expectedDuration) {
        // Arrange

        // Act
        PlanDuration duration = PlanDuration.fromDays(days);

        // Assert
        assertThat(duration).isEqualTo(expectedDuration);
        assertThat(duration.getDays()).isEqualTo(days);
    }

    @ParameterizedTest
    @MethodSource("invalidDurations")
    @DisplayName("Should throw DomainException SUB-005 when day count is not a supported plan duration")
    void shouldThrowDomainExceptionWhenDayCountIsNotSupported(int invalidDays) {
        // Arrange

        // Act & Assert
        assertThatThrownBy(() -> PlanDuration.fromDays(invalidDays))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("code", "SUB-005");
    }

    private static Stream<Arguments> validDurations() {
        return Stream.of(
                Arguments.of(15, PlanDuration.BIWEEKLY),
                Arguments.of(30, PlanDuration.MONTHLY));
    }

    private static Stream<Integer> invalidDurations() {
        return Stream.of(-1, 0, 14, 16, 29, 31);
    }
}
