package com.mcalvaro.subscription.event;

import com.mcalvaro.core.DomainEvent;
import com.mcalvaro.subscription.vo.PauseRange;

import java.time.Instant;
import java.util.UUID;

public record SubscriptionPaused(
        UUID eventId,
        Instant occurredAt,
        UUID subscriptionId,
        PauseRange pauseRange) implements DomainEvent {

    public SubscriptionPaused(UUID subscriptionId, PauseRange pauseRange) {
        this(UUID.randomUUID(), Instant.now(), subscriptionId, pauseRange);
    }
}
