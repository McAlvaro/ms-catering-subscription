package com.mcalvaro.mscatering.application.subscription.ReactivateSubscription;

import an.awesome.pipelinr.Command;

import com.mcalvaro.mscatering.application.abstractions.DomainEventDispatcher;
import com.mcalvaro.mscatering.domain.subscription.ISubscriptionRepository;
import com.mcalvaro.mscatering.domain.subscription.Subscription;

/**
 * Handler for {@link ReactivateSubscriptionCommand}.
 * <p>
 * Loads the paused subscription and delegates reactivation to the
 * aggregate root, which enforces that the subscription is in PAUSED state
 * and that an active pause request exists (INV-04).
 */
public class ReactivateSubscriptionCommandHandler implements Command.Handler<ReactivateSubscriptionCommand, Void> {

    private final ISubscriptionRepository subscriptionRepository;
    private final DomainEventDispatcher domainEventDispatcher;

    public ReactivateSubscriptionCommandHandler(ISubscriptionRepository subscriptionRepository,
            DomainEventDispatcher domainEventDispatcher) {
        this.subscriptionRepository = subscriptionRepository;
        this.domainEventDispatcher = domainEventDispatcher;
    }

    @Override
    public Void handle(ReactivateSubscriptionCommand command) {
        Subscription subscription = subscriptionRepository.findById(command.subscriptionId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Subscription not found: " + command.subscriptionId()));

        subscription.reactivate(command.reactivationDate());

        subscriptionRepository.save(subscription);
        domainEventDispatcher.dispatch();
        return null;
    }
}
