package com.mcalvaro.mscatering.infrastructure.persistence.patientaddress.repository;

import com.mcalvaro.mscatering.infrastructure.persistence.patientaddress.entity.PatientAddressReferenceJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringDataPatientAddressReferenceRepository
        extends JpaRepository<PatientAddressReferenceJpaEntity, UUID> {
}
