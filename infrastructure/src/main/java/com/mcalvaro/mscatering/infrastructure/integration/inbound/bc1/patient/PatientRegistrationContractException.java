package com.mcalvaro.mscatering.infrastructure.integration.inbound.bc1.patient;

/** Raised when the raw canonical BC1 registration contract is invalid. */
public class PatientRegistrationContractException extends RuntimeException {

    private final String code;

    public PatientRegistrationContractException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
