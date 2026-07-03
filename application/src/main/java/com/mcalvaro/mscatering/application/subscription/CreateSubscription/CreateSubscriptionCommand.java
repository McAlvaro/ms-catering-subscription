package com.mcalvaro.mscatering.application.subscription.CreateSubscription;

import an.awesome.pipelinr.Command;

import com.mcalvaro.mscatering.domain.subscription.vo.DeliveryPreferences;
import com.mcalvaro.mscatering.domain.subscription.vo.ServiceContract;

import java.util.UUID;

/**
 * Command to create a new catering subscription for a patient.
 * <p>
 * Maps to use case CU-01: Crear Suscripción de Catering.
 */
public record CreateSubscriptionCommand(
        UUID id,
        UUID patientId,
        UUID dietPlanId,
        ServiceContract contract,
        DeliveryPreferences preferences) implements Command<UUID> {
}
