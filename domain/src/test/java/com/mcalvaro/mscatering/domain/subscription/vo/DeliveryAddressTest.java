package com.mcalvaro.mscatering.domain.subscription.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.mcalvaro.mscatering.domain.core.DomainException;

public class DeliveryAddressTest {

    @Test
    @DisplayName("Should create delivery addrees successfully when all required fields are provided")
    void shouldCreateDeliveryAddressSuccessfully() {
        // Arrange

        // Act
        DeliveryAddress address = new DeliveryAddress(
                "street 123",
                "123",
                "Madrid",
                "Door color: blue",
                123.456,
                234.567,
                "678-90091");

        // Assert
        assertThat(address.street()).isEqualTo("street 123");
        assertThat(address.number()).isEqualTo("123");
        assertThat(address.city()).isEqualTo("Madrid");
        assertThat(address.reference()).isEqualTo("Door color: blue");
        assertThat(address.latitude()).isEqualTo(123.456);
        assertThat(address.longitude()).isEqualTo(234.567);
        assertThat(address.phone()).isEqualTo("678-90091");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "   ", "\t", "\n" })
    @DisplayName("Should throw DomainException V0-003 when street is null, empty or blank")
    void shouldThrowExceptionWhenStreetIsBlank(String invalidStreet) {

        // Act & Assert
        assertThatThrownBy(
                () -> new DeliveryAddress(
                        invalidStreet,
                        "123",
                        "Madrid",
                        "Door color: blue",
                        123.456,
                        234.567,
                        "678-90091"))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("code", "VO-003")
                .hasMessageContaining("street must not be blank");
    }
}
