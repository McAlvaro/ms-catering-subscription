package com.mcalvaro.mscatering.application.dietplan.synchronization;

import an.awesome.pipelinr.Command;
import com.mcalvaro.mscatering.domain.dietplan.DietPlanProjection;

import java.time.Instant;
import java.util.UUID;

public record SynchronizeDietPlanCommand(UUID eventId, String eventName, String eventVersion, Instant occurredOn,
        String source, String correlationId, DietPlanProjection plan, String payloadFingerprint)
        implements Command<Void> {
}
