package com.mcalvaro.mscatering.domain.subscription.event;

import com.mcalvaro.mscatering.domain.core.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Event emitted when a delivery fails to reach the patient.
 */
public record DeliveryFailed(
        UUID eventId,
        Instant occurredAt,
        UUID subscriptionId,
        UUID deliveryDayId,
        String reason) implements DomainEvent {

    public DeliveryFailed(UUID subscriptionId, UUID deliveryDayId, String reason) {
        this(UUID.randomUUID(), Instant.now(), subscriptionId, deliveryDayId, reason);
    }
}
