package com.mcalvaro.subscription.event;

import com.mcalvaro.core.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record SubscriptionCancelled(
        UUID eventId,
        Instant occurredAt,
        UUID subscriptionId,
        String reason) implements DomainEvent {

    public SubscriptionCancelled(UUID subscriptionId, String reason) {
        this(UUID.randomUUID(), Instant.now(), subscriptionId, reason);
    }
}
