package com.mcalvaro.mscatering.domain.subscription.service;

import com.mcalvaro.mscatering.domain.core.DomainException;
import com.mcalvaro.mscatering.domain.core.id.PatientId;

/**
 * Domain Service responsible for validating that a patient does not have
 * more than one ACTIVE or PAUSED subscription simultaneously.
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
    void validate(PatientId patientId);
}
