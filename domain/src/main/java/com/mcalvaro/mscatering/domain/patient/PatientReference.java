package com.mcalvaro.mscatering.domain.patient;

import com.mcalvaro.mscatering.domain.core.id.PatientId;

import java.time.Instant;

/**
 * Read model — local replica of a patient's activation status
 * (PacienteReferencia).
 * <p>
 * This is NOT an aggregate. It is a local projection of BC1 patient events
 * ({@code PacienteRegistrado}, {@code PacienteInactivado}) maintained by
 * the infrastructure layer via the message broker.
 * <p>
 * The domain may reference it to validate that a patient is active before
 * creating a subscription.
 */
public record PatientReference(
        PatientId patientId,
        String patientName,
        boolean active,
        Instant updatedAt) {

    public boolean isActive() {
        return active;
    }
}
