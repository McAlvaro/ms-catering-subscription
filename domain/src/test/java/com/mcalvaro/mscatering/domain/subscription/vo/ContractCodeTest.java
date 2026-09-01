package com.mcalvaro.mscatering.domain.subscription.vo;

import com.mcalvaro.mscatering.domain.core.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContractCodeTest {

    @Test
    @DisplayName("Should create a contract code when its format is valid")
    void shouldCreateContractCodeWhenFormatIsValid() {
        // Arrange
        String value = "NTC-2026-0001";

        // Act
        ContractCode contractCode = new ContractCode(value);

        // Assert
        assertThat(contractCode.value()).isEqualTo(value);
        assertThat(contractCode).isEqualTo(new ContractCode(value));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            " ",
            "NTC-2026-001",
            "NTC-2026-00001",
            "NTC-26-0001",
            "ABC-2026-0001",
            "ntc-2026-0001",
            "NTC-20A6-0001",
            "NTC-2026-00A1"
    })
    @DisplayName("Should throw DomainException SUB-007 when the contract code format is invalid")
    void shouldThrowDomainExceptionWhenContractCodeFormatIsInvalid(String invalidValue) {
        // Arrange
        // The invalid value is provided by the parameterized source.

        // Act & Assert
        assertThatThrownBy(() -> new ContractCode(invalidValue))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("code", "SUB-007")
                .hasMessageContaining("Expected NTC-YYYY-NNNN");
    }

    @Test
    @DisplayName("Should generate a contract code with a zero-padded sequence")
    void shouldGenerateZeroPaddedContractCodeWhenYearAndSequenceAreValid() {
        // Arrange
        int year = 2026;
        int sequence = 1;

        // Act
        ContractCode contractCode = ContractCode.generate(year, sequence);

        // Assert
        assertThat(contractCode).isEqualTo(new ContractCode("NTC-2026-0001"));
    }
}
