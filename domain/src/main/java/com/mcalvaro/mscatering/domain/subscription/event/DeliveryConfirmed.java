package com.mcalvaro.mscatering.domain.subscription.event;

import com.mcalvaro.mscatering.domain.core.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Event emitted when a delivery has been successfully completed.
 */
public record DeliveryConfirmed(
        UUID eventId,
        Instant occurredAt,
        UUID subscriptionId,
        UUID deliveryDayId) implements DomainEvent {

    public DeliveryConfirmed(UUID subscriptionId, UUID deliveryDayId) {
        this(UUID.randomUUID(), Instant.now(), subscriptionId, deliveryDayId);
    }
}
