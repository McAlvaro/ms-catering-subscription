package com.mcalvaro.mscatering.domain.subscription.event;

import com.mcalvaro.mscatering.domain.core.DomainEvent;
import com.mcalvaro.mscatering.domain.subscription.vo.ContractCode;
import com.mcalvaro.mscatering.domain.dietplan.PlanType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SubscriptionCreated(
        UUID eventId,
        Instant occurredAt,
        UUID subscriptionId,
        UUID patientId,
        ContractCode contractCode,
        UUID dietPlanId,
        PlanType planType,
        LocalDate periodStart,
        LocalDate periodEnd,
        int durationDays,
        List<UUID> evaluationIds,
        BigDecimal totalPrice,
        String correlationId) implements DomainEvent {

    public SubscriptionCreated(UUID subscriptionId, UUID patientId, ContractCode contractCode) {
        this(UUID.randomUUID(), Instant.now(), subscriptionId, patientId, contractCode, null, null,
                null, null, 0, List.of(), null, null);
    }

    public SubscriptionCreated(UUID subscriptionId, UUID patientId, ContractCode contractCode, UUID dietPlanId,
            PlanType planType, LocalDate periodStart, LocalDate periodEnd, int durationDays,
            List<UUID> evaluationIds, BigDecimal totalPrice, String correlationId) {
        this(UUID.randomUUID(), Instant.now(), subscriptionId, patientId, contractCode, dietPlanId, planType,
                periodStart, periodEnd, durationDays, List.copyOf(evaluationIds), totalPrice, correlationId);
    }
}
