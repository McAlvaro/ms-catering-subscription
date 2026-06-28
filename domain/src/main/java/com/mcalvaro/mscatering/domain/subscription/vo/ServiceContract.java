package com.mcalvaro.mscatering.domain.subscription.vo;

import com.mcalvaro.mscatering.domain.core.DomainException;
import com.mcalvaro.mscatering.domain.subscription.enums.PlanDuration;
import com.mcalvaro.mscatering.domain.subscription.enums.ServiceType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Value Object representing the signed service contract (ContratoDeServicio).
 * <p>
 * Validates:
 * <ul>
 * <li>{@code totalPrice} greater than zero</li>
 * <li>{@code acceptedConditions} non-blank</li>
 * <li>{@code period} non-null</li>
 * <li>{@code period.durationDays()} must match a valid {@link PlanDuration} (15
 * or 30 days)</li>
 * </ul>
 */
public record ServiceContract(
        UUID dietPlanId,
        ValidityPeriod period,
        ServiceType serviceType,
        BigDecimal totalPrice,
        String acceptedConditions,
        Instant signedAt) {

    public ServiceContract {
        if (period == null) {
            throw new DomainException("VO-009", "ServiceContract: period must not be null.");
        }
        // Delegates duration validation to PlanDuration — the single source of truth
        // for allowed plan lengths. Throws DomainException SUB-005 if invalid.
        PlanDuration.fromDays(period.durationDays());

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
