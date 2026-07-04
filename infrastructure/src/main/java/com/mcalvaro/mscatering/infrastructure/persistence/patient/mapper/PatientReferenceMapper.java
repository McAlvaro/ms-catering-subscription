package com.mcalvaro.mscatering.infrastructure.persistence.patient.mapper;

import com.mcalvaro.mscatering.domain.patient.PatientReference;
import com.mcalvaro.mscatering.infrastructure.persistence.patient.entity.PatientReferenceJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class PatientReferenceMapper {

    public PatientReferenceJpaEntity toJpaEntity(PatientReference domain) {
        if (domain == null) return null;

        PatientReferenceJpaEntity jpa = new PatientReferenceJpaEntity();
        jpa.setPatientId(domain.patientId());
        jpa.setActive(domain.active());
        jpa.setUpdatedAt(domain.updatedAt());
        return jpa;
    }

    public PatientReference toDomain(PatientReferenceJpaEntity jpa) {
        if (jpa == null) return null;

        return new PatientReference(
                jpa.getPatientId(),
                jpa.isActive(),
                jpa.getUpdatedAt()
        );
    }
}
