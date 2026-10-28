package com.mcalvaro.mscatering.domain.patient;

import java.util.Optional;
import java.util.UUID;

/** Port for the locally projected BC1 patient addresses. */
public interface IPatientAddressReferenceRepository {

    Optional<PatientAddressReference> findByAddressId(UUID addressId);
}
