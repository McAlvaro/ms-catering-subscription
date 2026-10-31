package com.mcalvaro.mscatering.domain.core;

import java.io.Serializable;
import java.util.UUID;

/**
 * Common interface for all strongly-typed domain identity value objects.
 */
public interface DomainId extends Serializable {

    int DEFAULT_TOKEN_LENGTH = 8;

    String value();

    static void validate(String value, String prefix, String idName) {
        if (value == null || value.isBlank()) {
            throw new DomainException("ID-001", idName + " must not be null or blank");
        }
        if (!value.startsWith(prefix)) {
            throw new DomainException("ID-002", idName + " must start with prefix '" + prefix + "', got: " + value);
        }
        if (value.trim().length() <= prefix.length()) {
            throw new DomainException("ID-003", idName + " must have a non-empty payload after prefix '" + prefix + "'");
        }
    }

    static String randomToken() {
        return randomToken(DEFAULT_TOKEN_LENGTH);
    }

    static String randomToken(int length) {
        if (length <= 0 || length > 32) {
            throw new IllegalArgumentException("Token length must be between 1 and 32, got: " + length);
        }
        return UUID.randomUUID().toString().replace("-", "").substring(0, length).toUpperCase();
    }
}
