package com.mcalvaro.mscatering.infrastructure.persistence.patient.repository;

import com.mcalvaro.mscatering.domain.core.id.PatientId;
import com.mcalvaro.mscatering.domain.patient.IPatientReferenceRepository;
import com.mcalvaro.mscatering.domain.patient.PatientReference;
import com.mcalvaro.mscatering.infrastructure.persistence.patient.entity.PatientReferenceJpaEntity;
import com.mcalvaro.mscatering.infrastructure.persistence.patient.mapper.PatientReferenceMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class JpaPatientReferenceRepository implements IPatientReferenceRepository {

    private final SpringDataPatientReferenceRepository springDataRepository;
    private final PatientReferenceMapper mapper;

    public JpaPatientReferenceRepository(
            SpringDataPatientReferenceRepository springDataRepository,
            PatientReferenceMapper mapper) {
        this.springDataRepository = springDataRepository;
        this.mapper = mapper;
    }

    @Override
    public void save(PatientReference patientReference) {
        PatientReferenceJpaEntity jpa = mapper.toJpaEntity(patientReference);
        springDataRepository.save(jpa);
    }

    @Override
    public Optional<PatientReference> findById(PatientId patientId) {
        return springDataRepository.findById(patientId.value())
                .map(mapper::toDomain);
    }
}
