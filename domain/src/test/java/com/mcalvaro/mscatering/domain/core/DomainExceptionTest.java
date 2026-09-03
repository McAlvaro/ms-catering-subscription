package com.mcalvaro.mscatering.domain.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DomainExceptionTest {

    @Test
    @DisplayName("Should expose the error code when created with a code")
    void shouldExposeCodeWhenCreatedWithCode() {
        // Arrange
        DomainException exception = new DomainException("SUB-001", "Subscription is invalid");

        // Act
        String result = exception.getCode();

        // Assert
        assertThat(result).isEqualTo("SUB-001");
        assertThat(exception).hasFieldOrPropertyWithValue("code", "SUB-001");
    }

    @Test
    @DisplayName("Should expose the error message when created with a message")
    void shouldExposeMessageWhenCreatedWithMessage() {
        // Arrange
        DomainException exception = new DomainException("SUB-001", "Subscription is invalid");

        // Act
        String result = exception.getMessage();

        // Assert
        assertThat(result).isEqualTo("Subscription is invalid");
    }
}
