package com.mcalvaro.mscatering.application.subscription.MarkEvaluationCompleted;

import an.awesome.pipelinr.Command;

import com.mcalvaro.mscatering.application.abstractions.DomainEventDispatcher;
import com.mcalvaro.mscatering.domain.subscription.ISubscriptionRepository;
import com.mcalvaro.mscatering.domain.subscription.Subscription;

public class MarkEvaluationCompletedCommandHandler implements Command.Handler<MarkEvaluationCompletedCommand, Void> {

    private final ISubscriptionRepository subscriptionRepository;
    private final DomainEventDispatcher domainEventDispatcher;

    public MarkEvaluationCompletedCommandHandler(ISubscriptionRepository subscriptionRepository,
            DomainEventDispatcher domainEventDispatcher) {
        this.subscriptionRepository = subscriptionRepository;
        this.domainEventDispatcher = domainEventDispatcher;
    }

    @Override
    public Void handle(MarkEvaluationCompletedCommand command) {
        Subscription subscription = subscriptionRepository.findById(command.subscriptionId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Subscription not found: " + command.subscriptionId()));

        subscription.markEvaluationCompleted(command.evaluationId(), command.completedAt());

        subscriptionRepository.save(subscription);
        domainEventDispatcher.dispatch();
        return null;
    }
}
