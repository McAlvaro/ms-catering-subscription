package com.mcalvaro.mscatering.application.subscription.CreateSubscription;

import an.awesome.pipelinr.Command;
import com.mcalvaro.mscatering.domain.core.id.AddressId;
import com.mcalvaro.mscatering.domain.core.id.DietPlanId;
import com.mcalvaro.mscatering.domain.core.id.PatientId;
import com.mcalvaro.mscatering.domain.core.id.SubscriptionId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Command to create a new catering subscription for a patient.
 * <p>
 * Maps to use case CU-01: Crear Suscripción de Catering.
 */
public record CreateSubscriptionCommand(
        PatientId patientId,
        DietPlanId dietPlanId,
        LocalDate startDate,
        LocalDate endDate,
        String serviceType,
        BigDecimal totalPrice,
        String acceptedConditions,
        AddressId deliveryAddressId,
        LocalTime prefTimeStart,
        LocalTime prefTimeEnd,
        String prefSpecialInstructions) implements Command<SubscriptionId> {

    public CreateSubscriptionCommand(String patientId, String dietPlanId, LocalDate startDate, LocalDate endDate,
            String serviceType, BigDecimal totalPrice, String acceptedConditions, String deliveryAddressId,
            LocalTime prefTimeStart, LocalTime prefTimeEnd, String prefSpecialInstructions) {
        this(patientId != null ? PatientId.of(patientId) : null,
                dietPlanId != null ? DietPlanId.of(dietPlanId) : null,
                startDate, endDate, serviceType, totalPrice, acceptedConditions,
                deliveryAddressId != null ? AddressId.of(deliveryAddressId) : null,
                prefTimeStart, prefTimeEnd, prefSpecialInstructions);
    }
}
