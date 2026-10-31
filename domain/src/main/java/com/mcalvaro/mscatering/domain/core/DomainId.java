package com.mcalvaro.mscatering.domain.core;

import java.io.Serializable;

/**
 * Common interface for all strongly-typed domain identity value objects.
 */
public interface DomainId extends Serializable {

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
}
