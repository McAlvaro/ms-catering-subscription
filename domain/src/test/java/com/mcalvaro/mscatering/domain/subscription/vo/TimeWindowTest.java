package com.mcalvaro.mscatering.domain.subscription.vo;

import com.mcalvaro.mscatering.domain.core.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalTime;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TimeWindowTest {

    @Test
    @DisplayName("Should create a time window when start time is before end time")
    void shouldCreateTimeWindowWhenStartTimeIsBeforeEndTime() {
        // Arrange
        LocalTime start = LocalTime.of(12, 0);
        LocalTime end = LocalTime.of(14, 0);

        // Act
        TimeWindow window = new TimeWindow(start, end);

        // Assert
        assertThat(window.startTime()).isEqualTo(start);
        assertThat(window.endTime()).isEqualTo(end);
        assertThat(window).isEqualTo(new TimeWindow(start, end));
    }

    @ParameterizedTest
    @MethodSource("windowsWithNullTime")
    @DisplayName("Should throw DomainException VO-001 when either time is null")
    void shouldThrowDomainExceptionWhenEitherTimeIsNull(LocalTime start, LocalTime end) {
        // Arrange
        // Times are provided by the method source.

        // Act & Assert
        assertThatThrownBy(() -> new TimeWindow(start, end))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("code", "VO-001")
                .hasMessageContaining("must not be null");
    }

    @ParameterizedTest
    @MethodSource("windowsNotInAscendingOrder")
    @DisplayName("Should throw DomainException VO-002 when start time is not before end time")
    void shouldThrowDomainExceptionWhenStartTimeIsNotBeforeEndTime(LocalTime start, LocalTime end) {
        // Arrange
        // Times are provided by the method source.

        // Act & Assert
        assertThatThrownBy(() -> new TimeWindow(start, end))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("code", "VO-002")
                .hasMessageContaining("startTime must be before endTime");
    }

    private static Stream<LocalTime[]> windowsWithNullTime() {
        LocalTime noon = LocalTime.NOON;
        return Stream.of(
                new LocalTime[] { null, noon },
                new LocalTime[] { noon, null },
                new LocalTime[] { null, null });
    }

    private static Stream<LocalTime[]> windowsNotInAscendingOrder() {
        LocalTime noon = LocalTime.NOON;
        return Stream.of(
                new LocalTime[] { noon, noon },
                new LocalTime[] { noon.plusHours(1), noon });
    }
}
