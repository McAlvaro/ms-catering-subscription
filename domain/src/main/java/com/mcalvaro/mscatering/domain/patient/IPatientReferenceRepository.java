package com.mcalvaro.mscatering.domain.patient;

import java.util.Optional;
import java.util.UUID;

public interface IPatientReferenceRepository {
    void save(PatientReference patientReference);

    Optional<PatientReference> findById(UUID patientId);
}
