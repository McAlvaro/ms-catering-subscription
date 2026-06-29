package com.mcalvaro.mscatering.domain.subscription.service;

import java.util.UUID;

import com.mcalvaro.mscatering.domain.core.DomainException;

/**
 * Domain Service responsible for validating that a patient does not have
 * more than one ACTIVE or PAUSED subscription simultaneously.
 * <p>
 * This logic requires cross-aggregate boundaries validation (querying the
 * repository)
 * and therefore cannot reside inside the Subscription Aggregate Root itself.
 */
public interface SubscriptionDuplicationValidator {

    /**
     * Validates if a patient is allowed to create a new subscription.
     * Implements rule INV-01.
     *
     * @param patientId The patient ID.
     * @throws DomainException if the patient already has an active or paused
     *                         subscription.
     */
    void validate(UUID patientId);
}
