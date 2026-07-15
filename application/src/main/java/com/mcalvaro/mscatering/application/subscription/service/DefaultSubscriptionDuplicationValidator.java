package com.mcalvaro.mscatering.application.subscription.service;

import com.mcalvaro.mscatering.domain.core.DomainException;
import com.mcalvaro.mscatering.domain.patient.IPatientReferenceRepository;
import com.mcalvaro.mscatering.domain.patient.PatientReference;
import com.mcalvaro.mscatering.domain.subscription.ISubscriptionRepository;
import com.mcalvaro.mscatering.domain.subscription.service.SubscriptionDuplicationValidator;

import java.util.UUID;

public class DefaultSubscriptionDuplicationValidator implements SubscriptionDuplicationValidator {

    private final ISubscriptionRepository subscriptionRepository;
    private final IPatientReferenceRepository patientRepository;

    public DefaultSubscriptionDuplicationValidator(
            ISubscriptionRepository subscriptionRepository,
            IPatientReferenceRepository patientRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.patientRepository = patientRepository;
    }

    @Override
    public void validate(UUID patientId) {
        // 1. Validar que el paciente exista y esté activo (Read Model local)
        PatientReference patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new DomainException("SUB-014", "Patient not found."));

        if (!patient.isActive()) {
            throw new DomainException("SUB-015", "Patient is inactive.");
        }

        // 2. Validar que el paciente no tenga una suscripción activa o pausada
        boolean hasDuplicate = subscriptionRepository.hasActiveOrPausedSubscription(patientId);

        if (hasDuplicate) {
            throw new DomainException("SUB-013", "Patient already has an ACTIVE or PAUSED subscription.");
        }
    }
}
