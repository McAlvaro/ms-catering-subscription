package com.mcalvaro.mscatering.application.subscription.MarkEvaluationCompleted;

import an.awesome.pipelinr.Command;
import com.mcalvaro.mscatering.domain.core.id.EvaluationId;
import com.mcalvaro.mscatering.domain.core.id.SubscriptionId;

import java.time.LocalDate;

/**
 * Command to mark a biweekly nutritional evaluation as completed.
 */
public record MarkEvaluationCompletedCommand(
        SubscriptionId subscriptionId,
        EvaluationId evaluationId,
        LocalDate completedAt) implements Command<Void> {

    public MarkEvaluationCompletedCommand(String subscriptionId, String evaluationId, LocalDate completedAt) {
        this(SubscriptionId.of(subscriptionId), EvaluationId.of(evaluationId), completedAt);
    }
}
