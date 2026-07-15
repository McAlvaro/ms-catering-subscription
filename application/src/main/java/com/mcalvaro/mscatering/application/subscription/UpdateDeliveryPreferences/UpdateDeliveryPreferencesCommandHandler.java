package com.mcalvaro.mscatering.application.subscription.UpdateDeliveryPreferences;

import an.awesome.pipelinr.Command;

import com.mcalvaro.mscatering.domain.subscription.ISubscriptionRepository;
import com.mcalvaro.mscatering.domain.subscription.Subscription;
import com.mcalvaro.mscatering.domain.subscription.vo.DeliveryAddress;
import com.mcalvaro.mscatering.domain.subscription.vo.DeliveryPreferences;
import com.mcalvaro.mscatering.domain.subscription.vo.TimeWindow;

public class UpdateDeliveryPreferencesCommandHandler
        implements Command.Handler<UpdateDeliveryPreferencesCommand, Void> {

    private final ISubscriptionRepository subscriptionRepository;

    public UpdateDeliveryPreferencesCommandHandler(ISubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    @Override
    public Void handle(UpdateDeliveryPreferencesCommand command) {
        Subscription subscription = subscriptionRepository.findById(command.subscriptionId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Subscription not found: " + command.subscriptionId()));

        DeliveryAddress address = new DeliveryAddress(
                command.street(),
                command.number(),
                command.city(),
                command.reference(),
                command.latitude(),
                command.longitude(),
                command.phone());
        TimeWindow window = new TimeWindow(command.startTime(), command.endTime());
        DeliveryPreferences preferences = new DeliveryPreferences(address, window, command.specialInstructions());

        subscription.updateDeliveryPreferences(preferences);

        subscriptionRepository.save(subscription);
        return null;
    }
}
