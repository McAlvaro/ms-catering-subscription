package com.mcalvaro.mscatering.application.subscription.UpdateDeliveryPreferences;

import an.awesome.pipelinr.Command;

import com.mcalvaro.mscatering.domain.subscription.vo.DeliveryPreferences;

import java.util.UUID;

/**
 * Command to update the default delivery preferences of a subscription.
 * Note: this only updates future generated days, not currently scheduled days,
 * unless specifically requested (business logic). Currently updates the root
 * VO.
 */
public record UpdateDeliveryPreferencesCommand(
        UUID subscriptionId,
        DeliveryPreferences newPreferences) implements Command<Void> {
}
