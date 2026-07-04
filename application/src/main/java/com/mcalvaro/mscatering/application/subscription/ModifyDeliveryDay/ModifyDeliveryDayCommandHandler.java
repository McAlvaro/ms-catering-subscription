package com.mcalvaro.mscatering.application.subscription.ModifyDeliveryDay;

import an.awesome.pipelinr.Command;

import com.mcalvaro.mscatering.application.abstractions.DomainEventDispatcher;
import com.mcalvaro.mscatering.domain.subscription.ISubscriptionRepository;
import com.mcalvaro.mscatering.domain.subscription.Subscription;

/**
 * Handler for {@link ModifyDeliveryDayCommand}.
 * <p>
 * Delegates to {@link Subscription#modifyDeliveryDay} which enforces
 * the 48-hour modification rule (RN-02) and the blocked-day invariant (INV-02).
 */
public class ModifyDeliveryDayCommandHandler implements Command.Handler<ModifyDeliveryDayCommand, Void> {

    private final ISubscriptionRepository subscriptionRepository;
    private final DomainEventDispatcher domainEventDispatcher;

    public ModifyDeliveryDayCommandHandler(ISubscriptionRepository subscriptionRepository,
            DomainEventDispatcher domainEventDispatcher) {
        this.subscriptionRepository = subscriptionRepository;
        this.domainEventDispatcher = domainEventDispatcher;
    }

    @Override
    public Void handle(ModifyDeliveryDayCommand command) {
        Subscription subscription = subscriptionRepository.findById(command.subscriptionId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Subscription not found: " + command.subscriptionId()));

        subscription.modifyDeliveryDay(
                command.deliveryDayId(),
                command.newAddress(),
                command.newTimeWindow(),
                command.newInstructions());

        subscriptionRepository.save(subscription);
        domainEventDispatcher.dispatch();
        return null;
    }
}
