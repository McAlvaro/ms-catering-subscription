package com.mcalvaro.mscatering.application.subscription.MarkNoDelivery;

import an.awesome.pipelinr.Command;

import com.mcalvaro.mscatering.application.abstractions.DomainEventDispatcher;
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
    private final DomainEventDispatcher domainEventDispatcher;

    public MarkNoDeliveryCommandHandler(ISubscriptionRepository subscriptionRepository,
            DomainEventDispatcher domainEventDispatcher) {
        this.subscriptionRepository = subscriptionRepository;
        this.domainEventDispatcher = domainEventDispatcher;
    }

    @Override
    public Void handle(MarkNoDeliveryCommand command) {
        Subscription subscription = subscriptionRepository.findById(command.subscriptionId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Subscription not found: " + command.subscriptionId()));

        subscription.markNoDelivery(command.deliveryDayId());

        subscriptionRepository.save(subscription);
        domainEventDispatcher.dispatch();
        return null;
    }
}
