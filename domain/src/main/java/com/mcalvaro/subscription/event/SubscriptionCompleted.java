package com.mcalvaro.subscription.event;

import com.mcalvaro.core.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record SubscriptionCompleted(
        UUID eventId,
        Instant occurredAt,
        UUID subscriptionId) implements DomainEvent {

    public SubscriptionCompleted(UUID subscriptionId) {
        this(UUID.randomUUID(), Instant.now(), subscriptionId);
    }
}
