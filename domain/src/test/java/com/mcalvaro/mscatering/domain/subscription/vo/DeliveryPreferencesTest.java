package com.mcalvaro.mscatering.domain.subscription.vo;

import com.mcalvaro.mscatering.domain.core.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeliveryPreferencesTest {

    @Test
    @DisplayName("Should create delivery preferences when address and time window are provided")
    void shouldCreateDeliveryPreferencesWhenAddressAndTimeWindowAreProvided() {
        // Arrange
        DeliveryAddress address = buildAddress();
        TimeWindow timeWindow = buildTimeWindow();
        String instructions = "Ring the bell once";

        // Act
        DeliveryPreferences preferences = new DeliveryPreferences(address, timeWindow, instructions);

        // Assert
        assertThat(preferences.primaryAddress()).isEqualTo(address);
        assertThat(preferences.timeWindow()).isEqualTo(timeWindow);
        assertThat(preferences.specialInstructions()).isEqualTo(instructions);
        assertThat(preferences).isEqualTo(new DeliveryPreferences(address, timeWindow, instructions));
    }

    @Test
    @DisplayName("Should allow null special instructions when required preferences are provided")
    void shouldAllowNullSpecialInstructionsWhenRequiredPreferencesAreProvided() {
        // Arrange
        DeliveryAddress address = buildAddress();
        TimeWindow timeWindow = buildTimeWindow();

        // Act
        DeliveryPreferences preferences = new DeliveryPreferences(address, timeWindow, null);

        // Assert
        assertThat(preferences.specialInstructions()).isNull();
    }

    @Test
    @DisplayName("Should throw DomainException VO-005 when primary address is null")
    void shouldThrowDomainExceptionWhenPrimaryAddressIsNull() {
        // Arrange
        TimeWindow timeWindow = buildTimeWindow();

        // Act & Assert
        assertThatThrownBy(() -> new DeliveryPreferences(null, timeWindow, "Instructions"))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("code", "VO-005")
                .hasMessageContaining("primaryAddress must not be null");
    }

    @Test
    @DisplayName("Should throw DomainException VO-006 when time window is null")
    void shouldThrowDomainExceptionWhenTimeWindowIsNull() {
        // Arrange
        DeliveryAddress address = buildAddress();

        // Act & Assert
        assertThatThrownBy(() -> new DeliveryPreferences(address, null, "Instructions"))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("code", "VO-006")
                .hasMessageContaining("timeWindow must not be null");
    }

    private DeliveryAddress buildAddress() {
        return new DeliveryAddress("Main Street", "10", "Madrid", null, 40.0, -3.0, "555-0100");
    }

    private TimeWindow buildTimeWindow() {
        return new TimeWindow(LocalTime.of(12, 0), LocalTime.of(14, 0));
    }
}
