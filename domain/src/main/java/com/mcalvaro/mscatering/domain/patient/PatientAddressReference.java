package com.mcalvaro.mscatering.domain.patient;

import java.util.UUID;

/**
 * Read model of an address published by BC1. It is not an aggregate in BC3.
 */
public record PatientAddressReference(
        UUID addressId,
        UUID patientId,
        String street,
        String reference,
        String city,
        double latitude,
        double longitude) {
}
