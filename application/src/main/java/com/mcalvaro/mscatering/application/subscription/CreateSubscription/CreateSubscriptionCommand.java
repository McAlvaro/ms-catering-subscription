package com.mcalvaro.mscatering.application.subscription.CreateSubscription;

import an.awesome.pipelinr.Command;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Command to create a new catering subscription for a patient.
 * <p>
 * Maps to use case CU-01: Crear Suscripción de Catering.
 * Contains only primitive/standard types — the Handler is responsible
 * for assembling the domain Value Objects.
 */
public record CreateSubscriptionCommand(
        UUID patientId,
        UUID dietPlanId,
        LocalDate startDate,
        LocalDate endDate,
        String serviceType,
        BigDecimal totalPrice,
        String acceptedConditions,
        String prefStreet,
        String prefNumber,
        String prefCity,
        String prefReference,
        double prefLatitude,
        double prefLongitude,
        String prefPhone,
        LocalTime prefTimeStart,
        LocalTime prefTimeEnd,
        String prefSpecialInstructions) implements Command<UUID> {
}
