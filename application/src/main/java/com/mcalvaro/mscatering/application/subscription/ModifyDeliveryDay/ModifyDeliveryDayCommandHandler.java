package com.mcalvaro.mscatering.application.subscription.ModifyDeliveryDay;

import an.awesome.pipelinr.Command;

import com.mcalvaro.mscatering.domain.subscription.ISubscriptionRepository;
import com.mcalvaro.mscatering.domain.subscription.Subscription;
import com.mcalvaro.mscatering.domain.subscription.vo.DeliveryAddress;
import com.mcalvaro.mscatering.domain.subscription.vo.TimeWindow;

/**
 * Handler for {@link ModifyDeliveryDayCommand}.
 * <p>
 * Delegates to {@link Subscription#modifyDeliveryDay} which enforces
 * the 48-hour modification rule (RN-02) and the blocked-day invariant (INV-02).
 */
public class ModifyDeliveryDayCommandHandler implements Command.Handler<ModifyDeliveryDayCommand, Void> {

    private final ISubscriptionRepository subscriptionRepository;

    public ModifyDeliveryDayCommandHandler(ISubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    @Override
    public Void handle(ModifyDeliveryDayCommand command) {
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

        subscription.modifyDeliveryDay(
                command.deliveryDayId(),
                address,
                window,
                command.instructions());

        subscriptionRepository.save(subscription);
        return null;
    }
}
