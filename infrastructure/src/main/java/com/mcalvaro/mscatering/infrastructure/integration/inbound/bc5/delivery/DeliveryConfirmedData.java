package com.mcalvaro.mscatering.infrastructure.integration.inbound.bc5.delivery;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Consumed payload of BC5 Logistics EntregaConfirmada event.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DeliveryConfirmedData(
        @JsonProperty("idEntrega") UUID idEntrega,
        @JsonProperty("idPaquete") UUID idPaquete,
        @JsonProperty("idCalendarioDia") UUID idCalendarioDia,
        @JsonProperty("idSuscripcion") UUID idSuscripcion,
        @JsonProperty("idPaciente") UUID idPaciente,
        @JsonProperty("fechaHoraEntrega") String fechaHoraEntrega,
        @JsonProperty("recibidoPor") String recibidoPor,
        @JsonProperty("evidenciaFotoUrl") String evidenciaFotoUrl,
        @JsonProperty("geolocalizacion") Geolocation geolocalizacion
) {

    public DeliveryConfirmedData(UUID idEntrega, UUID idPaquete, UUID idCalendarioDia, UUID idSuscripcion,
                                 UUID idPaciente, Instant fechaHoraEntrega, String recibidoPor,
                                 String evidenciaFotoUrl, Geolocation geolocalizacion) {
        this(idEntrega, idPaquete, idCalendarioDia, idSuscripcion, idPaciente,
                fechaHoraEntrega != null ? fechaHoraEntrega.toString() : null,
                recibidoPor, evidenciaFotoUrl, geolocalizacion);
    }

    public DeliveryConfirmedData(UUID idEntrega, UUID idPaquete, UUID idCalendarioDia, UUID idSuscripcion,
                                 UUID idPaciente, OffsetDateTime fechaHoraEntrega, String recibidoPor,
                                 String evidenciaFotoUrl, Geolocation geolocalizacion) {
        this(idEntrega, idPaquete, idCalendarioDia, idSuscripcion, idPaciente,
                fechaHoraEntrega != null ? fechaHoraEntrega.toString() : null,
                recibidoPor, evidenciaFotoUrl, geolocalizacion);
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Geolocation(
            @JsonProperty("latitud") Double latitude,
            @JsonProperty("longitud") Double longitude
    ) {
    }
}
