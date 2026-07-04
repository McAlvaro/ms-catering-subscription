package com.mcalvaro.mscatering.application.subscription.ConfirmDelivery;

import an.awesome.pipelinr.Command;

import com.mcalvaro.mscatering.domain.subscription.ISubscriptionRepository;
import com.mcalvaro.mscatering.domain.subscription.Subscription;

public class ConfirmDeliveryCommandHandler implements Command.Handler<ConfirmDeliveryCommand, Void> {

    private final ISubscriptionRepository subscriptionRepository;

    public ConfirmDeliveryCommandHandler(ISubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    @Override
    public Void handle(ConfirmDeliveryCommand command) {
        Subscription subscription = subscriptionRepository.findById(command.subscriptionId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Subscription not found: " + command.subscriptionId()));

        subscription.confirmDelivery(command.deliveryDayId());

        subscriptionRepository.save(subscription);
        return null;
    }
}
