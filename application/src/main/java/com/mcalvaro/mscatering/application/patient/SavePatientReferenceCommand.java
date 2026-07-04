package com.mcalvaro.mscatering.application.patient;

import an.awesome.pipelinr.Command;
import java.time.Instant;
import java.util.UUID;

public record SavePatientReferenceCommand(
        UUID patientId,
        boolean active,
        Instant updatedAt) implements Command<Void> {
}
