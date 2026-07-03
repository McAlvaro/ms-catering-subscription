package com.mcalvaro.mscatering.application.subscription.CancelSubscription;

import an.awesome.pipelinr.Command;

import com.mcalvaro.mscatering.application.abstractions.UnitOfWork;
import com.mcalvaro.mscatering.domain.subscription.ISubscriptionRepository;
import com.mcalvaro.mscatering.domain.subscription.Subscription;

/**
 * Handler for {@link CancelSubscriptionCommand}.
 * <p>
 * Delegates cancellation to the aggregate, which cascades the cancellation
 * to all SCHEDULED/PAUSED delivery days and PENDING evaluations.
 */
public class CancelSubscriptionCommandHandler implements Command.Handler<CancelSubscriptionCommand, Void> {

    private final ISubscriptionRepository subscriptionRepository;
    private final UnitOfWork unitOfWork;

    public CancelSubscriptionCommandHandler(ISubscriptionRepository subscriptionRepository,
            UnitOfWork unitOfWork) {
        this.subscriptionRepository = subscriptionRepository;
        this.unitOfWork = unitOfWork;
    }

    @Override
    public Void handle(CancelSubscriptionCommand command) {
        Subscription subscription = subscriptionRepository.findById(command.subscriptionId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Subscription not found: " + command.subscriptionId()));

        subscription.cancel(command.reason());

        subscriptionRepository.save(subscription);
        unitOfWork.commit();
        return null;
    }
}
