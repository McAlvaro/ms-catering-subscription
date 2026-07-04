package com.mcalvaro.mscatering.infrastructure.persistence.patient.repository;

import com.mcalvaro.mscatering.infrastructure.persistence.patient.entity.PatientReferenceJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SpringDataPatientReferenceRepository extends JpaRepository<PatientReferenceJpaEntity, UUID> {
}
