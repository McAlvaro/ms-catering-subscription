package com.mcalvaro.mscatering.infrastructure.integration.inbound.bc1.address;

import com.mcalvaro.mscatering.domain.core.DomainException;

public class PatientAddressRegistrationContractException extends DomainException {

    public PatientAddressRegistrationContractException(String code, String message) {
        super(code, message);
    }
}
