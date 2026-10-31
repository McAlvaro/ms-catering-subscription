package com.mcalvaro.mscatering.infrastructure.integration.inbound.bc5.delivery;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

/**
 * Consumed payload of BC5 Logistics EntregaNoConcretada event.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DeliveryFailedData(
        @JsonProperty("idEntrega") String idEntrega,
        @JsonProperty("idPaquete") String idPaquete,
        @JsonProperty("idCalendarioDia") String idCalendarioDia,
        @JsonProperty("idSuscripcion") String idSuscripcion,
        @JsonProperty("idPaciente") String idPaciente,
        @JsonProperty("fechaEntrega") LocalDate fechaEntrega,
        @JsonProperty("intentosRealizados") Integer intentosRealizados,
        @JsonProperty("motivoFinal") String motivoFinal,
        @JsonProperty("aplicaCompensacion") Boolean aplicaCompensacion,
        @JsonProperty("tipoCompensacion") String tipoCompensacion
) {

    public DeliveryFailedData(String idEntrega, String idPaquete, String idCalendarioDia, String idSuscripcion,
                              String idPaciente, String fechaEntrega, Integer intentosRealizados,
                              String motivoFinal, Boolean aplicaCompensacion, String tipoCompensacion) {
        this(idEntrega, idPaquete, idCalendarioDia, idSuscripcion, idPaciente,
                fechaEntrega != null ? LocalDate.parse(fechaEntrega) : null,
                intentosRealizados, motivoFinal, aplicaCompensacion, tipoCompensacion);
    }

    public String subscriptionId() {
        return idSuscripcion;
    }

    public String deliveryId() {
        return idEntrega;
    }

    public String packageId() {
        return idPaquete;
    }

    public String calendarDayId() {
        return idCalendarioDia;
    }

    public String patientId() {
        return idPaciente;
    }

    public String finalReason() {
        return motivoFinal;
    }
}
