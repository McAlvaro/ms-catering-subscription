package com.mcalvaro.mscatering.domain.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DomainIdTest {

    @Test
    @DisplayName("randomToken() generates an 8-character uppercase hex string")
    void shouldGenerateDefault8CharToken() {
        String token = DomainId.randomToken();

        assertThat(token)
                .isNotNull()
                .hasSize(8)
                .matches("^[0-9A-F]{8}$");
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 4, 8, 16, 32})
    @DisplayName("randomToken(length) generates token with specified valid length")
    void shouldGenerateTokenWithCustomLength(int length) {
        String token = DomainId.randomToken(length);

        assertThat(token)
                .isNotNull()
                .hasSize(length)
                .matches("^[0-9A-F]{" + length + "}$");
    }

    @ParameterizedTest
    @ValueSource(ints = {-5, 0, 33, 100})
    @DisplayName("randomToken(length) throws IllegalArgumentException on invalid length")
    void shouldThrowOnInvalidTokenLength(int invalidLength) {
        assertThatThrownBy(() -> DomainId.randomToken(invalidLength))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Token length must be between 1 and 32");
    }

    @Test
    @DisplayName("validate() throws ID-001 on null or blank value")
    void validateShouldThrowOnNullOrBlank() {
        assertThatThrownBy(() -> DomainId.validate(null, "SUS-", "SubscriptionId"))
                .isInstanceOf(DomainException.class)
                .satisfies(e -> assertThat(((DomainException) e).getCode()).isEqualTo("ID-001"));

        assertThatThrownBy(() -> DomainId.validate("   ", "SUS-", "SubscriptionId"))
                .isInstanceOf(DomainException.class)
                .satisfies(e -> assertThat(((DomainException) e).getCode()).isEqualTo("ID-001"));
    }

    @Test
    @DisplayName("validate() throws ID-002 on invalid prefix")
    void validateShouldThrowOnInvalidPrefix() {
        assertThatThrownBy(() -> DomainId.validate("INVALID-12345678", "SUS-", "SubscriptionId"))
                .isInstanceOf(DomainException.class)
                .satisfies(e -> assertThat(((DomainException) e).getCode()).isEqualTo("ID-002"));
    }

    @Test
    @DisplayName("validate() throws ID-003 on empty payload after prefix")
    void validateShouldThrowOnEmptyPayload() {
        assertThatThrownBy(() -> DomainId.validate("SUS-", "SUS-", "SubscriptionId"))
                .isInstanceOf(DomainException.class)
                .satisfies(e -> assertThat(((DomainException) e).getCode()).isEqualTo("ID-003"));
    }

    @Test
    @DisplayName("validate() succeeds on valid prefix and payload")
    void validateShouldSucceedOnValidValue() {
        DomainId.validate("SUS-A1B2C3D4", "SUS-", "SubscriptionId");
    }
}
