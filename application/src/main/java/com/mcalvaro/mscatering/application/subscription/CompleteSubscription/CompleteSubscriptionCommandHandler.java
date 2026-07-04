package com.mcalvaro.mscatering.application.subscription.CompleteSubscription;

import an.awesome.pipelinr.Command;

import com.mcalvaro.mscatering.application.abstractions.DomainEventDispatcher;
import com.mcalvaro.mscatering.domain.subscription.ISubscriptionRepository;
import com.mcalvaro.mscatering.domain.subscription.Subscription;

public class CompleteSubscriptionCommandHandler implements Command.Handler<CompleteSubscriptionCommand, Void> {

    private final ISubscriptionRepository subscriptionRepository;
    private final DomainEventDispatcher domainEventDispatcher;

    public CompleteSubscriptionCommandHandler(ISubscriptionRepository subscriptionRepository,
            DomainEventDispatcher domainEventDispatcher) {
        this.subscriptionRepository = subscriptionRepository;
        this.domainEventDispatcher = domainEventDispatcher;
    }

    @Override
    public Void handle(CompleteSubscriptionCommand command) {
        Subscription subscription = subscriptionRepository.findById(command.subscriptionId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Subscription not found: " + command.subscriptionId()));

        subscription.complete();

        subscriptionRepository.save(subscription);
        domainEventDispatcher.dispatch();
        return null;
    }
}
