package com.mcalvaro.mscatering.domain.consolidatedcalendar.entity;

import com.mcalvaro.mscatering.domain.core.id.ConsolidatedLineId;
import com.mcalvaro.mscatering.domain.core.id.DietPlanId;
import com.mcalvaro.mscatering.domain.core.id.PatientId;
import com.mcalvaro.mscatering.domain.core.id.SubscriptionId;
import com.mcalvaro.mscatering.domain.subscription.vo.DeliveryAddress;

import java.util.List;

/** Deep immutable copy of a closed delivery used by the close event. */
public record ConsolidatedDeliverySnapshot(
        ConsolidatedLineId calendarDayId,
        SubscriptionId subscriptionId,
        PatientId patientId,
        String patientName,
        DietPlanId dietPlanId,
        int planDayNumber,
        DeliveryAddress address,
        List<ConsolidatedRecipeSnapshot> recipes) {
    public ConsolidatedDeliverySnapshot {
        recipes = List.copyOf(recipes);
    }

    public ConsolidatedLineId lineId() {
        return calendarDayId;
    }
}
