package com.mcalvaro.mscatering.application.subscription.PauseSubscription;

import an.awesome.pipelinr.Command;

import com.mcalvaro.mscatering.domain.subscription.ISubscriptionRepository;
import com.mcalvaro.mscatering.domain.subscription.Subscription;

/**
 * Handler for {@link PauseSubscriptionCommand}.
 * <p>
 * Loads the subscription aggregate, delegates the pause operation
 * to the aggregate root (which enforces invariant INV-04 and the
 * 48-hour anticipation rule), and commits the transaction.
 */
public class PauseSubscriptionCommandHandler implements Command.Handler<PauseSubscriptionCommand, Void> {

    private final ISubscriptionRepository subscriptionRepository;

    public PauseSubscriptionCommandHandler(ISubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    @Override
    public Void handle(PauseSubscriptionCommand command) {
        Subscription subscription = subscriptionRepository.findById(command.subscriptionId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Subscription not found: " + command.subscriptionId()));

        subscription.pause(command.range(), command.reason());

        subscriptionRepository.save(subscription);
        return null;
    }
}
