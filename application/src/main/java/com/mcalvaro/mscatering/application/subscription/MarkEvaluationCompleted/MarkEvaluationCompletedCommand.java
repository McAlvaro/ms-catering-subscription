package com.mcalvaro.mscatering.application.subscription.MarkEvaluationCompleted;

import an.awesome.pipelinr.Command;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Command to mark a biweekly nutritional evaluation as completed.
 */
public record MarkEvaluationCompletedCommand(
        UUID subscriptionId,
        UUID evaluationId,
        LocalDate completedAt) implements Command<Void> {
}
