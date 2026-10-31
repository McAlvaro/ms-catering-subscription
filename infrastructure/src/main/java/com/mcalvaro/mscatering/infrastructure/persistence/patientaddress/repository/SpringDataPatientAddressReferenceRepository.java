package com.mcalvaro.mscatering.infrastructure.persistence.patientaddress.repository;

import com.mcalvaro.mscatering.infrastructure.persistence.patientaddress.entity.PatientAddressReferenceJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataPatientAddressReferenceRepository
        extends JpaRepository<PatientAddressReferenceJpaEntity, String> {
}
