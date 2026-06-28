package com.mcalvaro.mscatering.domain.subscription.vo;

import com.mcalvaro.mscatering.domain.core.DomainException;

import java.util.regex.Pattern;

/**
 * Value Object representing the unique, human-readable contract identifier
 * (CodigoDeContrato). Format: {@code NTC-YYYY-NNNN}.
 */
public record ContractCode(String value) {

    private static final Pattern FORMAT = Pattern.compile("^NTC-\\d{4}-\\d{4}$");

    public ContractCode {
        if (value == null || !FORMAT.matcher(value).matches()) {
            throw new DomainException("SUB-007",
                    "Invalid contract code format: " + value + ". Expected NTC-YYYY-NNNN.");
        }
    }

    /**
     * Generates a valid {@code ContractCode} from the given year and sequence
     * number.
     *
     * @param year     four-digit year (e.g. 2026)
     * @param sequence a number that will be zero-padded to 4 digits (e.g. 1 →
     *                 "0001")
     */
    public static ContractCode generate(int year, int sequence) {
        return new ContractCode("NTC-%d-%04d".formatted(year, sequence));
    }
}
