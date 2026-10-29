package com.mcalvaro.mscatering.integration.contracts;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

final class ContractAssertions {
    private ContractAssertions() {
    }

    static void envelope(UUID eventId, Instant occurredOn, String correlationId, Object data) {
        required(eventId, "eventId");
        required(occurredOn, "occurredOn");
        text(correlationId, "correlationId");
        required(data, "data");
    }

    static void required(Object value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " must not be null");
        }
    }

    static void text(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }

    static void date(LocalDate value, String name) {
        required(value, name);
    }

    static void positive(int value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
    }

    static void nonNegative(int value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must not be negative");
        }
    }

    static void nonNegative(BigDecimal value, String name) {
        required(value, name);
        if (value.signum() < 0) {
            throw new IllegalArgumentException(name + " must not be negative");
        }
    }

    static void positive(BigDecimal value, String name) {
        required(value, name);
        if (value.signum() <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
    }

    static <T> List<T> nonEmptyList(List<T> values, String name) {
        required(values, name);
        List<T> immutableValues = List.copyOf(values);
        if (immutableValues.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be empty");
        }
        return immutableValues;
    }

    static void coordinates(double latitude, double longitude) {
        if (!Double.isFinite(latitude) || latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("latitude must be between -90 and 90");
        }
        if (!Double.isFinite(longitude) || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("longitude must be between -180 and 180");
        }
    }
}
