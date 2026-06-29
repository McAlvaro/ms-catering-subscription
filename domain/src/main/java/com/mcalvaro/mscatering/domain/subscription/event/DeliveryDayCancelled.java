package com.mcalvaro.mscatering.domain.subscription.event;

import com.mcalvaro.mscatering.domain.core.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Event emitted when a patient proactively chooses not to receive
 * delivery for a specific day.
 */
public record DeliveryDayCancelled(
        UUID eventId,
        Instant occurredAt,
        UUID subscriptionId,
        UUID deliveryDayId) implements DomainEvent {

    public DeliveryDayCancelled(UUID subscriptionId, UUID deliveryDayId) {
        this(UUID.randomUUID(), Instant.now(), subscriptionId, deliveryDayId);
    }
}
