package com.mcalvaro.mscatering.infrastructure.service;

import com.mcalvaro.mscatering.domain.core.DomainException;
import com.mcalvaro.mscatering.domain.subscription.enums.SubscriptionStatus;
import com.mcalvaro.mscatering.domain.subscription.service.SubscriptionDuplicationValidator;
import com.mcalvaro.mscatering.infrastructure.persistence.patient.entity.PatientReferenceJpaEntity;
import com.mcalvaro.mscatering.infrastructure.persistence.patient.repository.SpringDataPatientReferenceRepository;
import com.mcalvaro.mscatering.infrastructure.persistence.subscription.repository.SpringDataSubscriptionRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class DefaultSubscriptionDuplicationValidator implements SubscriptionDuplicationValidator {

    private final SpringDataSubscriptionRepository subscriptionRepository;
    private final SpringDataPatientReferenceRepository patientRepository;

    public DefaultSubscriptionDuplicationValidator(
            SpringDataSubscriptionRepository subscriptionRepository,
            SpringDataPatientReferenceRepository patientRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.patientRepository = patientRepository;
    }

    @Override
    public void validate(UUID patientId) {
        // 1. Validar que el paciente exista y esté activo (Read Model local)
        PatientReferenceJpaEntity patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new DomainException("SUB-014", "Patient not found."));

        if (!patient.isActive()) {
            throw new DomainException("SUB-015", "Patient is inactive.");
        }

        // 2. Validar que el paciente no tenga una suscripción activa o pausada
        boolean hasDuplicate = subscriptionRepository.existsByPatientIdAndStatusIn(
                patientId,
                List.of(SubscriptionStatus.ACTIVE.name(), SubscriptionStatus.PAUSED.name()));

        if (hasDuplicate) {
            throw new DomainException("SUB-013", "Patient already has an ACTIVE or PAUSED subscription.");
        }
    }
}
