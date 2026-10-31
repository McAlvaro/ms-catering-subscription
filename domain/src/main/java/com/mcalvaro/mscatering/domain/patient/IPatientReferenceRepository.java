package com.mcalvaro.mscatering.domain.patient;

import com.mcalvaro.mscatering.domain.core.id.PatientId;

import java.util.Optional;

public interface IPatientReferenceRepository {
    void save(PatientReference patientReference);

    Optional<PatientReference> findById(PatientId patientId);
}
