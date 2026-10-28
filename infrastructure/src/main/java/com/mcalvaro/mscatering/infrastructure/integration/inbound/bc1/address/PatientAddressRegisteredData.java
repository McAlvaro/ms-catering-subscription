package com.mcalvaro.mscatering.infrastructure.integration.inbound.bc1.address;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

/**
 * Consumed portion of BC1's address payload; all other wire fields are ignored.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PatientAddressRegisteredData(
        @JsonProperty("idPaciente") UUID patientId,
        @JsonProperty("idDireccion") UUID addressId,
        @JsonProperty("direccion") String street,
        @JsonProperty("referencia") String reference,
        @JsonProperty("ciudad") String city,
        @JsonProperty("geolocalizacion") Geolocation geolocation) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Geolocation(
            @JsonProperty("latitud") Double latitude,
            @JsonProperty("longitud") Double longitude) {
    }
}
