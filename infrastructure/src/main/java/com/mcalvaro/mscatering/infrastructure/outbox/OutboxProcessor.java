package com.mcalvaro.mscatering.infrastructure.outbox;

import com.mcalvaro.mscatering.application.integration.IntegrationEventPublisher;
import com.mcalvaro.mscatering.application.outbox.OutboxMessage;
import com.mcalvaro.mscatering.application.outbox.OutboxMessageRepository;
import com.mcalvaro.mscatering.application.outbox.translation.OutboxEventTranslatorRegistry;
import com.mcalvaro.mscatering.infrastructure.persistence.outbox.codec.JacksonDomainEventCodec;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Dispatches one short-lived lease at a time; publishing never runs in a DB
 * transaction.
 */
public final class OutboxProcessor {
    private static final Duration INITIAL_BACKOFF = Duration.ofSeconds(5);
    private static final Duration MAX_BACKOFF = Duration.ofMinutes(5);

    private final OutboxMessageRepository repository;
    private final JacksonDomainEventCodec codec;
    private final OutboxEventTranslatorRegistry translators;
    private final IntegrationEventPublisher publisher;
    private final Clock clock;
    private final Duration initialBackoff;
    private final Duration maxBackoff;

    public OutboxProcessor(OutboxMessageRepository repository, JacksonDomainEventCodec codec,
            OutboxEventTranslatorRegistry translators, IntegrationEventPublisher publisher, Clock clock) {
        this(repository, codec, translators, publisher, clock, INITIAL_BACKOFF, MAX_BACKOFF);
    }

    public OutboxProcessor(OutboxMessageRepository repository, JacksonDomainEventCodec codec,
            OutboxEventTranslatorRegistry translators, IntegrationEventPublisher publisher, Clock clock,
            Duration initialBackoff, Duration maxBackoff) {
        this.repository = Objects.requireNonNull(repository, "Outbox repository must not be null");
        this.codec = Objects.requireNonNull(codec, "Domain event codec must not be null");
        this.translators = Objects.requireNonNull(translators, "Outbox translators must not be null");
        this.publisher = Objects.requireNonNull(publisher, "Integration event publisher must not be null");
        this.clock = Objects.requireNonNull(clock, "Clock must not be null");
        requirePositive(initialBackoff, "Initial backoff");
        requirePositive(maxBackoff, "Maximum backoff");
        if (initialBackoff.compareTo(maxBackoff) > 0) {
            throw new IllegalArgumentException("Initial backoff must not exceed maximum backoff");
        }
        this.initialBackoff = initialBackoff;
        this.maxBackoff = maxBackoff;
    }

    public void processAvailable(int batchSize, Duration leaseDuration) {
        if (batchSize <= 0) {
            throw new IllegalArgumentException("Batch size must be greater than zero");
        }
        requirePositive(leaseDuration, "Lease duration must be positive");
        Instant now = clock.instant();
        List<OutboxMessage> candidates = repository.findPendingAvailable(now, batchSize);
        for (OutboxMessage candidate : candidates) {
            processCandidate(candidate.id(), leaseDuration);
        }
    }

    private void processCandidate(UUID id, Duration leaseDuration) {
        Instant claimNow = clock.instant();
        String lockToken = UUID.randomUUID().toString();
        repository.tryClaim(id, claimNow, claimNow.plus(leaseDuration), lockToken)
                .ifPresent(message -> publishThenFinalize(message, lockToken));
    }

    private void publishThenFinalize(OutboxMessage message, String lockToken) {
        try {
            publisher.publish(translators.translate(codec.decode(message.type(), message.content())));
        } catch (Exception failure) {
            repository.rescheduleAfterFailureIfOwned(message.id(), lockToken,
                    clock.instant().plus(backoffFor(message.attemptCount(), initialBackoff, maxBackoff)),
                    formatError(failure));
            return;
        }
        // A failed acknowledgement is deliberately not retried here: the lease expiry
        // enables at-least-once recovery.
        try {
            repository.markProcessedIfOwned(message.id(), lockToken, clock.instant());
        } catch (Exception ignored) {
            // The integration event was already published; rescheduling could cause an
            // immediate duplicate.
        }
    }

    static Duration backoffFor(int attemptCountBeforeFailure) {
        return backoffFor(attemptCountBeforeFailure, INITIAL_BACKOFF, MAX_BACKOFF);
    }

    private static Duration backoffFor(int attemptCountBeforeFailure, Duration initialBackoff, Duration maxBackoff) {
        if (attemptCountBeforeFailure < 0) {
            throw new IllegalArgumentException("Attempt count must not be negative");
        }
        Duration delay = initialBackoff;
        for (int attempt = 0; attempt < attemptCountBeforeFailure && delay.compareTo(maxBackoff) < 0; attempt++) {
            delay = delay.multipliedBy(2);
            if (delay.compareTo(maxBackoff) > 0) {
                delay = maxBackoff;
            }
        }
        return delay;
    }

    static String formatError(Exception failure) {
        String message = failure.getMessage();
        return failure.getClass().getName() + (message == null || message.isBlank() ? "" : ": " + message);
    }

    private static void requirePositive(Duration duration, String message) {
        Objects.requireNonNull(duration, message + " must not be null");
        if (duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException(message);
        }
    }
}
