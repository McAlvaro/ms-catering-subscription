package com.mcalvaro.mscatering.infrastructure.outbox;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

@ConfigurationProperties("outbox")
public record OutboxRelayProperties(@DefaultValue("none") String provider,
        @DefaultValue("false") boolean relayEnabled,
        @DefaultValue("5s") Duration pollInterval,
        @DefaultValue("20") int batchSize,
        @DefaultValue("1m") Duration leaseDuration,
        @DefaultValue("5s") Duration initialBackoff,
        @DefaultValue("5m") Duration maxBackoff) {

    public OutboxRelayProperties {
        if (provider == null || provider.isBlank()) {
            throw new IllegalArgumentException("Outbox provider must not be blank");
        }
        requirePositive(pollInterval, "Poll interval");
        if (batchSize <= 0) {
            throw new IllegalArgumentException("Batch size must be greater than zero");
        }
        requirePositive(leaseDuration, "Lease duration");
        requirePositive(initialBackoff, "Initial backoff");
        requirePositive(maxBackoff, "Maximum backoff");
        if (initialBackoff.compareTo(maxBackoff) > 0) {
            throw new IllegalArgumentException("Initial backoff must not exceed maximum backoff");
        }
    }

    private static void requirePositive(Duration value, String name) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(name + " must be positive");
        }
    }
}
