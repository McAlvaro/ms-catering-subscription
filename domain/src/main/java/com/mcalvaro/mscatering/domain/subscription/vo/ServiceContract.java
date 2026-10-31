package com.mcalvaro.mscatering.domain.subscription.vo;

import com.mcalvaro.mscatering.domain.core.DomainException;
import com.mcalvaro.mscatering.domain.core.id.DietPlanId;
import com.mcalvaro.mscatering.domain.subscription.enums.PlanDuration;
import com.mcalvaro.mscatering.domain.subscription.enums.ServiceType;
import com.mcalvaro.mscatering.domain.dietplan.PlanType;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Value Object representing the signed service contract (ContratoDeServicio).
 */
public record ServiceContract(
        DietPlanId dietPlanId,
        PlanType planType,
        ValidityPeriod period,
        ServiceType serviceType,
        BigDecimal totalPrice,
        String acceptedConditions,
        Instant signedAt) {

    public ServiceContract {
        if (period == null) {
            throw new DomainException("VO-009", "ServiceContract: period must not be null.");
        }
        if (planType == null) {
            throw new DomainException("VO-013", "ServiceContract: plan type must not be null.");
        }
        if (totalPrice == null || totalPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new DomainException("SUB-006", "Contract price must be greater than zero.");
        }
        if (acceptedConditions == null || acceptedConditions.isBlank()) {
            throw new DomainException("VO-010", "ServiceContract: acceptedConditions must not be blank.");
        }
        if (serviceType == null) {
            throw new DomainException("VO-011", "ServiceContract: serviceType must not be null.");
        }
        if (signedAt == null) {
            throw new DomainException("VO-012", "ServiceContract: signedAt must not be null.");
        }
    }
}
