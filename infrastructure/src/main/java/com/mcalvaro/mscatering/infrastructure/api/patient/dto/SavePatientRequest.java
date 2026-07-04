package com.mcalvaro.mscatering.infrastructure.api.patient.dto;

import java.time.Instant;
import java.util.UUID;

public record SavePatientRequest(
        UUID patientId,
        boolean active,
        Instant updatedAt) {
}
