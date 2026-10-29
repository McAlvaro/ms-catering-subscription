package com.mcalvaro.mscatering.application.outbox;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

/**
 * Durable outbox message. Content is opaque; encoding and event rehydration are
 * intentionally outside this application model.
 */
public record OutboxMessage(UUID id, String type, String content, Instant created, boolean processed,
        Instant processedOn,
        int attemptCount, String lastError, Instant availableAt, Instant lockedUntil, String lockedBy,
        String correlationId, String traceId, String spanId) {

    public OutboxMessage {
        Objects.requireNonNull(id, "Outbox message id must not be null");
        requireNonBlank(type, "Outbox message type must not be blank");
        requireNonBlank(content, "Outbox message content must not be blank");
        created = normalize(Objects.requireNonNull(created, "Outbox message created must not be null"));
        availableAt = normalize(Objects.requireNonNull(availableAt, "Outbox message availableAt must not be null"));
        processedOn = processedOn == null ? null : normalize(processedOn);
        lockedUntil = lockedUntil == null ? null : normalize(lockedUntil);

        if (attemptCount < 0) {
            throw new IllegalArgumentException("Outbox message attemptCount must not be negative");
        }
        if (processed != (processedOn != null)) {
            throw new IllegalArgumentException("Outbox message processed state must match processedOn");
        }
        if ((lockedUntil == null) != (lockedBy == null)) {
            throw new IllegalArgumentException("Outbox message lock fields must be populated together");
        }
        if (processed && lockedUntil != null) {
            throw new IllegalArgumentException("A processed outbox message cannot remain locked");
        }
        if (availableAt.isBefore(created)) {
            throw new IllegalArgumentException("Outbox message availableAt must not precede created");
        }
        if (processedOn != null && processedOn.isBefore(created)) {
            throw new IllegalArgumentException("Outbox message processedOn must not precede created");
        }
        requireOptionalNonBlank(lastError, "Outbox message lastError must not be blank");
        requireOptionalNonBlank(lockedBy, "Outbox message lockedBy must not be blank");
        requireOptionalNonBlank(correlationId, "Outbox message correlationId must not be blank");
        requireOptionalNonBlank(traceId, "Outbox message traceId must not be blank");
        requireOptionalNonBlank(spanId, "Outbox message spanId must not be blank");
    }

    public static OutboxMessage pending(UUID id, String type, String content, Instant created) {
        return new OutboxMessage(id, type, content, created, false, null, 0, null, created, null, null, null, null,
                null);
    }

    private static Instant normalize(Instant value) {
        return value.truncatedTo(ChronoUnit.MICROS);
    }

    private static void requireNonBlank(String value, String message) {
        if (value == null) {
            throw new NullPointerException(message);
        }
        if (value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    private static void requireOptionalNonBlank(String value, String message) {
        if (value != null && value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }
}
