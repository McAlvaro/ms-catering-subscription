package com.mcalvaro.mscatering.infrastructure.integration.inbound.bc5.delivery;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.time.OffsetDateTime;

/**
 * Consumed payload of BC5 Logistics EntregaConfirmada event.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DeliveryConfirmedData(
        @JsonProperty("idEntrega") String idEntrega,
        @JsonProperty("idPaquete") String idPaquete,
        @JsonProperty("idCalendarioDia") String idCalendarioDia,
        @JsonProperty("idSuscripcion") String idSuscripcion,
        @JsonProperty("idPaciente") String idPaciente,
        @JsonProperty("fechaHoraEntrega") String fechaHoraEntrega,
        @JsonProperty("recibidoPor") String recibidoPor,
        @JsonProperty("evidenciaFotoUrl") String evidenciaFotoUrl,
        @JsonProperty("geolocalizacion") Geolocation geolocalizacion
) {

    public DeliveryConfirmedData(String idEntrega, String idPaquete, String idCalendarioDia, String idSuscripcion,
                                 String idPaciente, Instant fechaHoraEntrega, String recibidoPor,
                                 String evidenciaFotoUrl, Geolocation geolocalizacion) {
        this(idEntrega, idPaquete, idCalendarioDia, idSuscripcion, idPaciente,
                fechaHoraEntrega != null ? fechaHoraEntrega.toString() : null,
                recibidoPor, evidenciaFotoUrl, geolocalizacion);
    }

    public DeliveryConfirmedData(String idEntrega, String idPaquete, String idCalendarioDia, String idSuscripcion,
                                 String idPaciente, OffsetDateTime fechaHoraEntrega, String recibidoPor,
                                 String evidenciaFotoUrl, Geolocation geolocalizacion) {
        this(idEntrega, idPaquete, idCalendarioDia, idSuscripcion, idPaciente,
                fechaHoraEntrega != null ? fechaHoraEntrega.toString() : null,
                recibidoPor, evidenciaFotoUrl, geolocalizacion);
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Geolocation(
            @JsonProperty("latitud") Double latitude,
            @JsonProperty("longitud") Double longitude
    ) {
    }
}
