package com.mcalvaro.mscatering.application.subscription.RegisterFailedDelivery;

import an.awesome.pipelinr.Command;

import com.mcalvaro.mscatering.domain.subscription.ISubscriptionRepository;
import com.mcalvaro.mscatering.domain.subscription.Subscription;

public class RegisterFailedDeliveryCommandHandler implements Command.Handler<RegisterFailedDeliveryCommand, Void> {

    private final ISubscriptionRepository subscriptionRepository;

    public RegisterFailedDeliveryCommandHandler(ISubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    @Override
    public Void handle(RegisterFailedDeliveryCommand command) {
        Subscription subscription = subscriptionRepository.findById(command.subscriptionId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Subscription not found: " + command.subscriptionId()));

        subscription.registerFailedDelivery(command.deliveryDayId(), command.reason());

        subscriptionRepository.save(subscription);
        return null;
    }
}
