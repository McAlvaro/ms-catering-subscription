package com.mcalvaro.mscatering.application.patient.synchronization;

/** Raised when an inbound patient synchronization consistency rule fails. */
public class PatientReferenceSynchronizationException extends RuntimeException {

    private final String code;

    public PatientReferenceSynchronizationException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
