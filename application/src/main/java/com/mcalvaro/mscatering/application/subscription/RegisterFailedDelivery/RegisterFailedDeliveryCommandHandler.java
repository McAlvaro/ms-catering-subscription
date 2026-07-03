package com.mcalvaro.mscatering.application.subscription.RegisterFailedDelivery;

import an.awesome.pipelinr.Command;

import com.mcalvaro.mscatering.application.abstractions.UnitOfWork;
import com.mcalvaro.mscatering.domain.subscription.ISubscriptionRepository;
import com.mcalvaro.mscatering.domain.subscription.Subscription;

public class RegisterFailedDeliveryCommandHandler implements Command.Handler<RegisterFailedDeliveryCommand, Void> {

    private final ISubscriptionRepository subscriptionRepository;
    private final UnitOfWork unitOfWork;

    public RegisterFailedDeliveryCommandHandler(ISubscriptionRepository subscriptionRepository,
            UnitOfWork unitOfWork) {
        this.subscriptionRepository = subscriptionRepository;
        this.unitOfWork = unitOfWork;
    }

    @Override
    public Void handle(RegisterFailedDeliveryCommand command) {
        Subscription subscription = subscriptionRepository.findById(command.subscriptionId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Subscription not found: " + command.subscriptionId()));

        subscription.registerFailedDelivery(command.deliveryDayId(), command.reason());

        subscriptionRepository.save(subscription);
        unitOfWork.commit();
        return null;
    }
}
