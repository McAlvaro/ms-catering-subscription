package com.mcalvaro.mscatering.application.subscription.UpdateDeliveryPreferences;

import an.awesome.pipelinr.Command;

import com.mcalvaro.mscatering.application.abstractions.UnitOfWork;
import com.mcalvaro.mscatering.domain.subscription.ISubscriptionRepository;
import com.mcalvaro.mscatering.domain.subscription.Subscription;

public class UpdateDeliveryPreferencesCommandHandler
        implements Command.Handler<UpdateDeliveryPreferencesCommand, Void> {

    private final ISubscriptionRepository subscriptionRepository;
    private final UnitOfWork unitOfWork;

    public UpdateDeliveryPreferencesCommandHandler(ISubscriptionRepository subscriptionRepository,
            UnitOfWork unitOfWork) {
        this.subscriptionRepository = subscriptionRepository;
        this.unitOfWork = unitOfWork;
    }

    @Override
    public Void handle(UpdateDeliveryPreferencesCommand command) {
        Subscription subscription = subscriptionRepository.findById(command.subscriptionId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Subscription not found: " + command.subscriptionId()));

        subscription.updateDeliveryPreferences(command.newPreferences());

        subscriptionRepository.save(subscription);
        unitOfWork.commit();
        return null;
    }
}
