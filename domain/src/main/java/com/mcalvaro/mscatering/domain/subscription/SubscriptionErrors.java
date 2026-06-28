package com.mcalvaro.mscatering.domain.subscription;

import com.mcalvaro.mscatering.domain.core.DomainException;

/**
 * Catalog of domain exceptions for the Subscription aggregate.
 * All methods are factory methods that return a pre-configured
 * {@link DomainException}.
 */
public final class SubscriptionErrors {

    private SubscriptionErrors() {
    }

    public static DomainException pauseRequires48HoursAnticipation() {
        return new DomainException("SUB-001", "Pause must be requested at least 48 hours in advance.");
    }

    public static DomainException cannotModifyDeliveryWithin48Hours() {
        return new DomainException("SUB-002", "Delivery day cannot be modified within 48 hours of delivery.");
    }

    public static DomainException subscriptionNotActive() {
        return new DomainException("SUB-003", "Operation requires an ACTIVE subscription.");
    }

    public static DomainException subscriptionNotPaused() {
        return new DomainException("SUB-004", "Operation requires a PAUSED subscription.");
    }

    public static DomainException invalidPlanDuration(int days) {
        return new DomainException("SUB-005", "Plan duration must be 15 or 30 days, got: " + days);
    }

    public static DomainException contractPriceInvalid() {
        return new DomainException("SUB-006", "Contract price must be greater than zero.");
    }

    public static DomainException invalidContractCode(String code) {
        return new DomainException("SUB-007", "Invalid contract code format: " + code);
    }

    public static DomainException subscriptionPeriodNotEnded() {
        return new DomainException("SUB-008", "Cannot complete subscription: contract period has not ended yet.");
    }

    public static DomainException deliveryDayNotFound(java.util.UUID dayId) {
        return new DomainException("SUB-009", "Delivery day not found: " + dayId);
    }

    public static DomainException noPauseRequestActive() {
        return new DomainException("SUB-010", "No active pause request found for this subscription.");
    }
}
