package com.mcalvaro.mscatering.domain.subscription.vo;

import com.mcalvaro.mscatering.domain.core.DomainException;
import com.mcalvaro.mscatering.domain.subscription.enums.ServiceType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ServiceContractTest {

    @Test
    @DisplayName("Should create a service contract when all required values are valid")
    void shouldCreateServiceContractWhenAllRequiredValuesAreValid() {
        // Arrange
        UUID dietPlanId = UUID.fromString("4f60d6f7-677a-45e7-b538-dac4bbb50ca4");
        ValidityPeriod period = buildPeriod();
        ServiceType serviceType = ServiceType.FULL;
        BigDecimal totalPrice = new BigDecimal("250.00");
        String conditions = "Terms accepted";
        Instant signedAt = Instant.parse("2026-01-01T10:00:00Z");

        // Act
        ServiceContract contract = new ServiceContract(
                dietPlanId, period, serviceType, totalPrice, conditions, signedAt);

        // Assert
        assertThat(contract.dietPlanId()).isEqualTo(dietPlanId);
        assertThat(contract.period()).isEqualTo(period);
        assertThat(contract.serviceType()).isEqualTo(serviceType);
        assertThat(contract.totalPrice()).isEqualByComparingTo(totalPrice);
        assertThat(contract.acceptedConditions()).isEqualTo(conditions);
        assertThat(contract.signedAt()).isEqualTo(signedAt);
        assertThat(contract).isEqualTo(new ServiceContract(
                dietPlanId, period, serviceType, totalPrice, conditions, signedAt));
    }

    @Test
    @DisplayName("Should allow a null diet plan identifier because it is not a required contract value")
    void shouldAllowNullDietPlanIdentifierWhenOtherValuesAreValid() {
        // Arrange
        ValidityPeriod period = buildPeriod();

        // Act
        ServiceContract contract = new ServiceContract(
                null,
                period,
                ServiceType.FULL,
                BigDecimal.ONE,
                "Terms accepted",
                Instant.parse("2026-01-01T10:00:00Z"));

        // Assert
        assertThat(contract.dietPlanId()).isNull();
    }

    @Test
    @DisplayName("Should accept a validity period whose duration is not a standard plan duration")
    void shouldAcceptValidityPeriodWhenDurationIsNotStandardPlanDuration() {
        // Arrange
        LocalDate start = LocalDate.of(2026, 1, 1);
        ValidityPeriod extendedPeriod = new ValidityPeriod(start, start.plusDays(17));

        // Act
        ServiceContract contract = new ServiceContract(
                UUID.randomUUID(),
                extendedPeriod,
                ServiceType.FULL,
                BigDecimal.ONE,
                "Terms accepted",
                Instant.parse("2026-01-01T10:00:00Z"));

        // Assert
        assertThat(contract.period()).isEqualTo(extendedPeriod);
    }

    @Test
    @DisplayName("Should throw DomainException VO-009 when validity period is null")
    void shouldThrowDomainExceptionWhenValidityPeriodIsNull() {
        // Arrange
        UUID dietPlanId = UUID.randomUUID();

        // Act & Assert
        assertThatThrownBy(() -> new ServiceContract(
                dietPlanId, null, ServiceType.FULL, BigDecimal.ONE, "Terms", Instant.now()))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("code", "VO-009")
                .hasMessageContaining("period must not be null");
    }

    @ParameterizedTest
    @MethodSource("nonPositivePrices")
    @DisplayName("Should throw DomainException SUB-006 when total price is null, zero or negative")
    void shouldThrowDomainExceptionWhenTotalPriceIsNullZeroOrNegative(BigDecimal invalidPrice) {
        // Arrange
        ValidityPeriod period = buildPeriod();

        // Act & Assert
        assertThatThrownBy(() -> new ServiceContract(
                UUID.randomUUID(), period, ServiceType.FULL, invalidPrice, "Terms", Instant.now()))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("code", "SUB-006")
                .hasMessageContaining("greater than zero");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { " ", "\t", "\n" })
    @DisplayName("Should throw DomainException VO-010 when accepted conditions are null, empty or blank")
    void shouldThrowDomainExceptionWhenAcceptedConditionsAreNullEmptyOrBlank(String invalidConditions) {
        // Arrange
        ValidityPeriod period = buildPeriod();

        // Act & Assert
        assertThatThrownBy(() -> new ServiceContract(
                UUID.randomUUID(), period, ServiceType.FULL, BigDecimal.ONE, invalidConditions, Instant.now()))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("code", "VO-010")
                .hasMessageContaining("acceptedConditions must not be blank");
    }

    @Test
    @DisplayName("Should throw DomainException VO-011 when service type is null")
    void shouldThrowDomainExceptionWhenServiceTypeIsNull() {
        // Arrange
        ValidityPeriod period = buildPeriod();

        // Act & Assert
        assertThatThrownBy(() -> new ServiceContract(
                UUID.randomUUID(), period, null, BigDecimal.ONE, "Terms", Instant.now()))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("code", "VO-011")
                .hasMessageContaining("serviceType must not be null");
    }

    @Test
    @DisplayName("Should throw DomainException VO-012 when signing instant is null")
    void shouldThrowDomainExceptionWhenSigningInstantIsNull() {
        // Arrange
        ValidityPeriod period = buildPeriod();

        // Act & Assert
        assertThatThrownBy(() -> new ServiceContract(
                UUID.randomUUID(), period, ServiceType.FULL, BigDecimal.ONE, "Terms", null))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("code", "VO-012")
                .hasMessageContaining("signedAt must not be null");
    }

    private static Stream<BigDecimal> nonPositivePrices() {
        return Stream.of((BigDecimal) null, BigDecimal.ZERO, new BigDecimal("-0.01"));
    }

    private ValidityPeriod buildPeriod() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        return new ValidityPeriod(start, start.plusDays(14));
    }
}
