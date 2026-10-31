package com.mcalvaro.mscatering.domain.subscription.event;

import com.mcalvaro.mscatering.domain.core.DomainEvent;
import com.mcalvaro.mscatering.domain.core.id.DeliveryDayId;
import com.mcalvaro.mscatering.domain.core.id.SubscriptionId;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record DeliveryDayModified(
        UUID eventId,
        Instant occurredAt,
        SubscriptionId subscriptionId,
        DeliveryDayId deliveryDayId,
        LocalDate date) implements DomainEvent {

    public DeliveryDayModified(SubscriptionId subscriptionId, DeliveryDayId deliveryDayId, LocalDate date) {
        this(UUID.randomUUID(), Instant.now(), subscriptionId, deliveryDayId, date);
    }
}
