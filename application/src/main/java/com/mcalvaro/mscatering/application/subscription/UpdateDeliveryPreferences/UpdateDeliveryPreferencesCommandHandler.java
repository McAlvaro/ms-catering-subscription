package com.mcalvaro.mscatering.application.subscription.UpdateDeliveryPreferences;

import an.awesome.pipelinr.Command;

import com.mcalvaro.mscatering.domain.subscription.ISubscriptionRepository;
import com.mcalvaro.mscatering.domain.subscription.Subscription;
import com.mcalvaro.mscatering.domain.subscription.vo.DeliveryAddress;
import com.mcalvaro.mscatering.domain.subscription.vo.DeliveryPreferences;
import com.mcalvaro.mscatering.domain.subscription.vo.TimeWindow;
import com.mcalvaro.mscatering.application.patient.address.DeliveryAddressResolver;

public class UpdateDeliveryPreferencesCommandHandler
        implements Command.Handler<UpdateDeliveryPreferencesCommand, Void> {

    private final ISubscriptionRepository subscriptionRepository;
    private final DeliveryAddressResolver deliveryAddressResolver;

    public UpdateDeliveryPreferencesCommandHandler(ISubscriptionRepository subscriptionRepository,
            DeliveryAddressResolver deliveryAddressResolver) {
        this.subscriptionRepository = subscriptionRepository;
        this.deliveryAddressResolver = deliveryAddressResolver;
    }

    @Override
    public Void handle(UpdateDeliveryPreferencesCommand command) {
        Subscription subscription = subscriptionRepository.findById(command.subscriptionId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Subscription not found: " + command.subscriptionId()));

        DeliveryAddress address = deliveryAddressResolver.resolve(subscription.getPatientId(),
                command.deliveryAddressId());
        TimeWindow window = new TimeWindow(command.startTime(), command.endTime());
        DeliveryPreferences preferences = new DeliveryPreferences(address, window, command.specialInstructions());

        subscription.updateDeliveryPreferences(preferences);

        subscriptionRepository.save(subscription);
        return null;
    }
}
