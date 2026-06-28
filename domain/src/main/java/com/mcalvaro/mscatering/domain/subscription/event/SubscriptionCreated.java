package com.mcalvaro.mscatering.domain.subscription.event;

import com.mcalvaro.mscatering.domain.core.DomainEvent;
import com.mcalvaro.mscatering.domain.subscription.vo.ContractCode;

import java.time.Instant;
import java.util.UUID;

public record SubscriptionCreated(
        UUID eventId,
        Instant occurredAt,
        UUID subscriptionId,
        UUID patientId,
        ContractCode contractCode) implements DomainEvent {

    public SubscriptionCreated(UUID subscriptionId, UUID patientId, ContractCode contractCode) {
        this(UUID.randomUUID(), Instant.now(), subscriptionId, patientId, contractCode);
    }
}
