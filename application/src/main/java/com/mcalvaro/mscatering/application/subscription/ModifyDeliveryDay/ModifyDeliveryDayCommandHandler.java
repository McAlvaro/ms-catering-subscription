package com.mcalvaro.mscatering.application.subscription.ModifyDeliveryDay;

import an.awesome.pipelinr.Command;

import com.mcalvaro.mscatering.application.abstractions.UnitOfWork;
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
    private final UnitOfWork unitOfWork;

    public ModifyDeliveryDayCommandHandler(ISubscriptionRepository subscriptionRepository,
            UnitOfWork unitOfWork) {
        this.subscriptionRepository = subscriptionRepository;
        this.unitOfWork = unitOfWork;
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
        unitOfWork.commit();
        return null;
    }
}
