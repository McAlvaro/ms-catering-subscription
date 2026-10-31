package com.mcalvaro.mscatering.domain.subscription.event;

import com.mcalvaro.mscatering.domain.core.CorrelatedDomainEvent;
import com.mcalvaro.mscatering.domain.core.id.DietPlanId;
import com.mcalvaro.mscatering.domain.core.id.EvaluationId;
import com.mcalvaro.mscatering.domain.core.id.PatientId;
import com.mcalvaro.mscatering.domain.core.id.SubscriptionId;
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
        SubscriptionId subscriptionId,
        PatientId patientId,
        ContractCode contractCode,
        DietPlanId dietPlanId,
        PlanType planType,
        LocalDate periodStart,
        LocalDate periodEnd,
        int durationDays,
        List<EvaluationId> evaluationIds,
        BigDecimal totalPrice,
        String correlationId) implements CorrelatedDomainEvent {

    public SubscriptionCreated(SubscriptionId subscriptionId, PatientId patientId, ContractCode contractCode) {
        this(UUID.randomUUID(), Instant.now(), subscriptionId, patientId, contractCode, null, null,
                null, null, 0, List.of(), null, null);
    }

    public SubscriptionCreated(SubscriptionId subscriptionId, PatientId patientId, ContractCode contractCode, DietPlanId dietPlanId,
            PlanType planType, LocalDate periodStart, LocalDate periodEnd, int durationDays,
            List<EvaluationId> evaluationIds, BigDecimal totalPrice, String correlationId) {
        this(UUID.randomUUID(), Instant.now(), subscriptionId, patientId, contractCode, dietPlanId, planType,
                periodStart, periodEnd, durationDays, List.copyOf(evaluationIds), totalPrice, correlationId);
    }
}
