package com.mcalvaro.mscatering.domain.subscription.event;

import com.mcalvaro.mscatering.domain.core.DomainEvent;
import com.mcalvaro.mscatering.domain.core.id.DeliveryDayId;
import com.mcalvaro.mscatering.domain.core.id.SubscriptionId;

import java.time.Instant;
import java.util.UUID;

/**
 * Event emitted when a patient proactively chooses not to receive
 * delivery for a specific day.
 */
public record DeliveryDayCancelled(
        UUID eventId,
        Instant occurredAt,
        SubscriptionId subscriptionId,
        DeliveryDayId deliveryDayId) implements DomainEvent {

    public DeliveryDayCancelled(SubscriptionId subscriptionId, DeliveryDayId deliveryDayId) {
        this(UUID.randomUUID(), Instant.now(), subscriptionId, deliveryDayId);
    }
}
