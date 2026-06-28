package com.mcalvaro.consolidatedcalendar.entity;

import com.mcalvaro.core.Entity;
import com.mcalvaro.subscription.enums.ServiceType;
import com.mcalvaro.subscription.vo.DeliveryAddress;
import com.mcalvaro.subscription.vo.TimeWindow;

import java.util.UUID;

/**
 * Entity representing a single delivery line in the consolidated calendar
 * (LineaConsolidado).
 * <p>
 * Reuses {@link DeliveryAddress} and {@link TimeWindow} Value Objects
 * from the subscription package — sharing VOs within the same bounded
 * context is correct DDD practice.
 */
public class ConsolidatedLine extends Entity {

    private final UUID consolidatedCalendarId;
    private final UUID subscriptionId;
    private final UUID patientId;
    private final UUID dietPlanId;
    private final ServiceType serviceType;
    private final DeliveryAddress address;
    private final TimeWindow timeWindow;
    private final String instructions;

    public ConsolidatedLine(UUID id, UUID consolidatedCalendarId, UUID subscriptionId,
            UUID patientId, UUID dietPlanId, ServiceType serviceType,
            DeliveryAddress address, TimeWindow timeWindow, String instructions) {
        super(id);
        this.consolidatedCalendarId = consolidatedCalendarId;
        this.subscriptionId = subscriptionId;
        this.patientId = patientId;
        this.dietPlanId = dietPlanId;
        this.serviceType = serviceType;
        this.address = address;
        this.timeWindow = timeWindow;
        this.instructions = instructions;
    }

    public UUID getConsolidatedCalendarId() {
        return consolidatedCalendarId;
    }

    public UUID getSubscriptionId() {
        return subscriptionId;
    }

    public UUID getPatientId() {
        return patientId;
    }

    public UUID getDietPlanId() {
        return dietPlanId;
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
