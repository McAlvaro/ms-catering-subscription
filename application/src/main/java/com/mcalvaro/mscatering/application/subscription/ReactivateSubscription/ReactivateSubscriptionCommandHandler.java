package com.mcalvaro.mscatering.application.subscription.ReactivateSubscription;

import an.awesome.pipelinr.Command;

import com.mcalvaro.mscatering.application.abstractions.UnitOfWork;
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
    private final UnitOfWork unitOfWork;

    public ReactivateSubscriptionCommandHandler(ISubscriptionRepository subscriptionRepository,
            UnitOfWork unitOfWork) {
        this.subscriptionRepository = subscriptionRepository;
        this.unitOfWork = unitOfWork;
    }

    @Override
    public Void handle(ReactivateSubscriptionCommand command) {
        Subscription subscription = subscriptionRepository.findById(command.subscriptionId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Subscription not found: " + command.subscriptionId()));

        subscription.reactivate(command.reactivationDate());

        subscriptionRepository.save(subscription);
        unitOfWork.commit();
        return null;
    }
}
