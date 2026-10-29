package com.mcalvaro.mscatering.domain.consolidatedcalendar.entity;

import com.mcalvaro.mscatering.domain.subscription.vo.DeliveryAddress;

import java.util.List;
import java.util.UUID;

/** Deep immutable copy of a closed delivery used by the close event. */
public record ConsolidatedDeliverySnapshot(UUID calendarDayId, UUID subscriptionId, UUID patientId,
        String patientName, UUID dietPlanId, int planDayNumber, DeliveryAddress address,
        List<ConsolidatedRecipeSnapshot> recipes) {
    public ConsolidatedDeliverySnapshot {
        recipes = List.copyOf(recipes);
    }
}
