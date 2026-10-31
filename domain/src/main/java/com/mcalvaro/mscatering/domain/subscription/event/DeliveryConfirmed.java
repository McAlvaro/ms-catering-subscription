package com.mcalvaro.mscatering.domain.subscription.event;

import com.mcalvaro.mscatering.domain.core.DomainEvent;
import com.mcalvaro.mscatering.domain.core.id.DeliveryDayId;
import com.mcalvaro.mscatering.domain.core.id.SubscriptionId;

import java.time.Instant;
import java.util.UUID;

/**
 * Event emitted when a delivery has been successfully completed.
 */
public record DeliveryConfirmed(
        UUID eventId,
        Instant occurredAt,
        SubscriptionId subscriptionId,
        DeliveryDayId deliveryDayId) implements DomainEvent {

    public DeliveryConfirmed(SubscriptionId subscriptionId, DeliveryDayId deliveryDayId) {
        this(UUID.randomUUID(), Instant.now(), subscriptionId, deliveryDayId);
    }
}
