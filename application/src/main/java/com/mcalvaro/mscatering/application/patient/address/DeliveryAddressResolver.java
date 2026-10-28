package com.mcalvaro.mscatering.application.patient.address;

import com.mcalvaro.mscatering.domain.core.DomainException;
import com.mcalvaro.mscatering.domain.patient.IPatientAddressReferenceRepository;
import com.mcalvaro.mscatering.domain.subscription.vo.DeliveryAddress;

import java.util.UUID;

/**
 * Resolves a BC1 address projection into the immutable delivery snapshot VO.
 */
public class DeliveryAddressResolver {

    public static final String ADDRESS_NOT_AVAILABLE_MESSAGE = "Delivery address is not available for the patient";

    private final IPatientAddressReferenceRepository addressRepository;

    public DeliveryAddressResolver(IPatientAddressReferenceRepository addressRepository) {
        this.addressRepository = addressRepository;
    }

    public DeliveryAddress resolve(UUID patientId, UUID addressId) {
        if (patientId == null || addressId == null) {
            throw new DomainException("ADR-001", ADDRESS_NOT_AVAILABLE_MESSAGE);
        }
        return addressRepository.findByAddressId(addressId)
                .filter(address -> address.patientId().equals(patientId))
                .map(address -> new DeliveryAddress(address.street(), null, address.city(), address.reference(),
                        address.latitude(), address.longitude(), null))
                .orElseThrow(() -> new DomainException("ADR-001", ADDRESS_NOT_AVAILABLE_MESSAGE));
    }
}
