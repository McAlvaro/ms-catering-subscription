package com.mcalvaro.subscription.event;

import com.mcalvaro.core.DomainEvent;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record DeliveryDayModified(
        UUID eventId,
        Instant occurredAt,
        UUID subscriptionId,
        UUID deliveryDayId,
        LocalDate date) implements DomainEvent {

    public DeliveryDayModified(UUID subscriptionId, UUID deliveryDayId, LocalDate date) {
        this(UUID.randomUUID(), Instant.now(), subscriptionId, deliveryDayId, date);
    }
}
