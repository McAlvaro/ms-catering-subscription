package com.mcalvaro.mscatering.infrastructure.api.subscription.dto;

import com.mcalvaro.mscatering.domain.subscription.enums.ServiceType;
import com.mcalvaro.mscatering.infrastructure.shared.validator.ValueOfEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record CreateSubscriptionRequest(
        @NotNull(message = "El ID del paciente es obligatorio")
        UUID patientId,

        @NotNull(message = "El ID del plan de dieta es obligatorio")
        UUID dietPlanId,

        @NotNull(message = "La fecha de inicio es obligatoria")
        LocalDate startDate,

        @NotNull(message = "La fecha de fin es obligatoria")
        LocalDate endDate,

        @NotNull(message = "El tipo de servicio es obligatorio")
        @ValueOfEnum(enumClass = ServiceType.class)
        ServiceType serviceType,

        @NotNull(message = "El precio total es obligatorio")
        BigDecimal totalPrice,

        @NotBlank(message = "Debe aceptar las condiciones")
        String acceptedConditions,

        @NotBlank(message = "La calle de entrega es obligatoria")
        String prefStreet,

        @NotBlank(message = "El número de entrega es obligatorio")
        String prefNumber,

        @NotBlank(message = "La ciudad de entrega es obligatoria")
        String prefCity,

        String prefReference,

        double prefLatitude,

        double prefLongitude,

        @NotBlank(message = "El teléfono de entrega es obligatorio")
        String prefPhone,

        @NotNull(message = "La hora de inicio de entrega es obligatoria")
        LocalTime prefTimeStart,

        @NotNull(message = "La hora de fin de entrega es obligatoria")
        LocalTime prefTimeEnd,

        String prefSpecialInstructions
) {
}
