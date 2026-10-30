package com.mcalvaro.mscatering.infrastructure.integration.inbound.bc5.delivery;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Consumed payload of BC5 Logistics EntregaNoConcretada event.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DeliveryFailedData(
        @JsonProperty("idEntrega") UUID idEntrega,
        @JsonProperty("idPaquete") UUID idPaquete,
        @JsonProperty("idCalendarioDia") UUID idCalendarioDia,
        @JsonProperty("idSuscripcion") UUID idSuscripcion,
        @JsonProperty("idPaciente") UUID idPaciente,
        @JsonProperty("fechaEntrega") LocalDate fechaEntrega,
        @JsonProperty("intentosRealizados") Integer intentosRealizados,
        @JsonProperty("motivoFinal") String motivoFinal,
        @JsonProperty("aplicaCompensacion") Boolean aplicaCompensacion,
        @JsonProperty("tipoCompensacion") String tipoCompensacion
) {

    public DeliveryFailedData(UUID idEntrega, UUID idPaquete, UUID idCalendarioDia, UUID idSuscripcion,
                              UUID idPaciente, String fechaEntrega, Integer intentosRealizados,
                              String motivoFinal, Boolean aplicaCompensacion, String tipoCompensacion) {
        this(idEntrega, idPaquete, idCalendarioDia, idSuscripcion, idPaciente,
                fechaEntrega != null ? LocalDate.parse(fechaEntrega) : null,
                intentosRealizados, motivoFinal, aplicaCompensacion, tipoCompensacion);
    }

    public UUID subscriptionId() {
        return idSuscripcion;
    }

    public UUID deliveryId() {
        return idEntrega;
    }

    public UUID packageId() {
        return idPaquete;
    }

    public UUID calendarDayId() {
        return idCalendarioDia;
    }

    public UUID patientId() {
        return idPaciente;
    }

    public String finalReason() {
        return motivoFinal;
    }
}
