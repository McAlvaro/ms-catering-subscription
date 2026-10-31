package com.mcalvaro.mscatering.domain.consolidatedcalendar.entity;

import com.mcalvaro.mscatering.domain.core.DomainException;
import com.mcalvaro.mscatering.domain.core.Entity;
import com.mcalvaro.mscatering.domain.core.id.ConsolidatedCalendarId;
import com.mcalvaro.mscatering.domain.core.id.ConsolidatedLineId;
import com.mcalvaro.mscatering.domain.core.id.DietPlanId;
import com.mcalvaro.mscatering.domain.core.id.PatientId;
import com.mcalvaro.mscatering.domain.core.id.SubscriptionId;
import com.mcalvaro.mscatering.domain.subscription.enums.ServiceType;
import com.mcalvaro.mscatering.domain.subscription.vo.DeliveryAddress;
import com.mcalvaro.mscatering.domain.subscription.vo.TimeWindow;

import java.util.List;

/**
 * Entity representing a single delivery line in the consolidated calendar
 * (LineaConsolidado).
 * <p>
 * Reuses {@link DeliveryAddress} and {@link TimeWindow} Value Objects
 * from the subscription package — sharing VOs within the same bounded
 * context is correct DDD practice.
 */
public class ConsolidatedLine extends Entity<ConsolidatedLineId> {

    private final ConsolidatedCalendarId consolidatedCalendarId;
    private final SubscriptionId subscriptionId;
    private final PatientId patientId;
    private final String patientName;
    private final DietPlanId dietPlanId;
    private final int planDayNumber;
    private final ServiceType serviceType;
    private final DeliveryAddress address;
    private final TimeWindow timeWindow;
    private final String instructions;
    private final List<ConsolidatedRecipeSnapshot> recipes;

    public ConsolidatedLine(ConsolidatedLineId id, ConsolidatedCalendarId consolidatedCalendarId, SubscriptionId subscriptionId, PatientId patientId,
            String patientName, DietPlanId dietPlanId, int planDayNumber, ServiceType serviceType,
            DeliveryAddress address, TimeWindow timeWindow, String instructions,
            List<ConsolidatedRecipeSnapshot> recipes) {
        super(id);
        this.consolidatedCalendarId = consolidatedCalendarId;
        this.subscriptionId = subscriptionId;
        this.patientId = patientId;
        this.patientName = patientName;
        this.dietPlanId = dietPlanId;
        this.planDayNumber = planDayNumber;
        this.serviceType = serviceType;
        this.address = address;
        this.timeWindow = timeWindow;
        this.instructions = instructions;
        this.recipes = recipes == null ? List.of() : List.copyOf(recipes);
        validateSnapshot();
    }

    public ConsolidatedCalendarId getConsolidatedCalendarId() {
        return consolidatedCalendarId;
    }

    public SubscriptionId getSubscriptionId() {
        return subscriptionId;
    }

    public PatientId getPatientId() {
        return patientId;
    }

    public DietPlanId getDietPlanId() {
        return dietPlanId;
    }

    public String getPatientName() {
        return patientName;
    }

    public int getPlanDayNumber() {
        return planDayNumber;
    }

    public List<ConsolidatedRecipeSnapshot> getRecipes() {
        return recipes;
    }

    public ConsolidatedDeliverySnapshot snapshot() {
        return new ConsolidatedDeliverySnapshot(getId(), subscriptionId, patientId, patientName, dietPlanId,
                planDayNumber, address, recipes);
    }

    private void validateSnapshot() {
        if (consolidatedCalendarId == null || subscriptionId == null || patientId == null || patientName == null
                || patientName.isBlank() || dietPlanId == null || planDayNumber < 1 || serviceType == null
                || address == null || address.addressId() == null || address.alias() == null || address.alias().isBlank()
                || address.reference() == null || address.reference().isBlank() || recipes.isEmpty()) {
            throw new DomainException("CAL-004", "Delivery snapshot is incomplete");
        }
    }

    public ServiceType getServiceType() {
        return serviceType;
    }

    public DeliveryAddress getAddress() {
        return address;
    }

    public TimeWindow getTimeWindow() {
        return timeWindow;
    }

    public String getInstructions() {
        return instructions;
    }
}
