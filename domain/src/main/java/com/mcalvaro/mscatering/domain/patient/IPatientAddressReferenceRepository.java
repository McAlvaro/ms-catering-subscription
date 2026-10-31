package com.mcalvaro.mscatering.domain.patient;

import com.mcalvaro.mscatering.domain.core.id.AddressId;

import java.util.Optional;

/** Port for the locally projected BC1 patient addresses. */
public interface IPatientAddressReferenceRepository {

    Optional<PatientAddressReference> findByAddressId(AddressId addressId);
}
