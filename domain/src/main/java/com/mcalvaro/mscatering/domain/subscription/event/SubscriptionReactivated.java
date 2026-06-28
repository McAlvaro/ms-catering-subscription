package com.mcalvaro.mscatering.domain.subscription.event;

import com.mcalvaro.mscatering.domain.core.DomainEvent;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record SubscriptionReactivated(
        UUID eventId,
        Instant occurredAt,
        UUID subscriptionId,
        LocalDate reactivatedOn) implements DomainEvent {

    public SubscriptionReactivated(UUID subscriptionId, LocalDate reactivatedOn) {
        this(UUID.randomUUID(), Instant.now(), subscriptionId, reactivatedOn);
    }
}
