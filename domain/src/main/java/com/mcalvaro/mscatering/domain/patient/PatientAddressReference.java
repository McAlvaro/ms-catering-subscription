package com.mcalvaro.mscatering.domain.patient;

import com.mcalvaro.mscatering.domain.core.id.AddressId;
import com.mcalvaro.mscatering.domain.core.id.PatientId;

/**
 * Read model of an address published by BC1. It is not an aggregate in BC3.
 */
public record PatientAddressReference(
        AddressId addressId,
        PatientId patientId,
        String alias,
        String street,
        String reference,
        String city,
        double latitude,
        double longitude) {

}
