package com.mcalvaro.mscatering.application.subscription.MarkEvaluationCompleted;

import an.awesome.pipelinr.Command;

import com.mcalvaro.mscatering.application.abstractions.UnitOfWork;
import com.mcalvaro.mscatering.domain.subscription.ISubscriptionRepository;
import com.mcalvaro.mscatering.domain.subscription.Subscription;

public class MarkEvaluationCompletedCommandHandler implements Command.Handler<MarkEvaluationCompletedCommand, Void> {

    private final ISubscriptionRepository subscriptionRepository;
    private final UnitOfWork unitOfWork;

    public MarkEvaluationCompletedCommandHandler(ISubscriptionRepository subscriptionRepository,
            UnitOfWork unitOfWork) {
        this.subscriptionRepository = subscriptionRepository;
        this.unitOfWork = unitOfWork;
    }

    @Override
    public Void handle(MarkEvaluationCompletedCommand command) {
        Subscription subscription = subscriptionRepository.findById(command.subscriptionId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Subscription not found: " + command.subscriptionId()));

        subscription.markEvaluationCompleted(command.evaluationId(), command.completedAt());

        subscriptionRepository.save(subscription);
        unitOfWork.commit();
        return null;
    }
}
