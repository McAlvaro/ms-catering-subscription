package com.mcalvaro.mscatering.application.subscription.MarkNoDelivery;

import an.awesome.pipelinr.Command;

import com.mcalvaro.mscatering.domain.subscription.ISubscriptionRepository;
import com.mcalvaro.mscatering.domain.subscription.Subscription;

/**
 * Handler for {@link MarkNoDeliveryCommand}.
 * <p>
 * Delegates to {@link Subscription#markNoDelivery} which enforces
 * invariant INV-05: the calendar cannot have all days as NO_DELIVERY.
 */
public class MarkNoDeliveryCommandHandler implements Command.Handler<MarkNoDeliveryCommand, Void> {

    private final ISubscriptionRepository subscriptionRepository;

    public MarkNoDeliveryCommandHandler(ISubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    @Override
    public Void handle(MarkNoDeliveryCommand command) {
        Subscription subscription = subscriptionRepository.findById(command.subscriptionId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Subscription not found: " + command.subscriptionId()));

        subscription.markNoDelivery(command.deliveryDayId());

        subscriptionRepository.save(subscription);
        return null;
    }
}
