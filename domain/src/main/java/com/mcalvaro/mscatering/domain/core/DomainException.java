package com.mcalvaro.mscatering.domain.core;

/**
 * Domain exception thrown when a business invariant is violated.
 * <p>
 * Carries a short machine-readable {@code code} (e.g. {@code "SUB-001"})
 * and a human-readable {@code message}. The application layer is
 * responsible for catching this exception and converting it to the
 * appropriate response (e.g. a {@code Result.failure(...)}).
 */
public class DomainException extends RuntimeException {

    private final String code;

    public DomainException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
