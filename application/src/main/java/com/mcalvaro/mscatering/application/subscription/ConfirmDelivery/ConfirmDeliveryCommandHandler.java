package com.mcalvaro.mscatering.application.subscription.ConfirmDelivery;

import an.awesome.pipelinr.Command;

import com.mcalvaro.mscatering.application.abstractions.UnitOfWork;
import com.mcalvaro.mscatering.domain.subscription.ISubscriptionRepository;
import com.mcalvaro.mscatering.domain.subscription.Subscription;

public class ConfirmDeliveryCommandHandler implements Command.Handler<ConfirmDeliveryCommand, Void> {

    private final ISubscriptionRepository subscriptionRepository;
    private final UnitOfWork unitOfWork;

    public ConfirmDeliveryCommandHandler(ISubscriptionRepository subscriptionRepository,
            UnitOfWork unitOfWork) {
        this.subscriptionRepository = subscriptionRepository;
        this.unitOfWork = unitOfWork;
    }

    @Override
    public Void handle(ConfirmDeliveryCommand command) {
        Subscription subscription = subscriptionRepository.findById(command.subscriptionId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Subscription not found: " + command.subscriptionId()));

        subscription.confirmDelivery(command.deliveryDayId());

        subscriptionRepository.save(subscription);
        unitOfWork.commit();
        return null;
    }
}
