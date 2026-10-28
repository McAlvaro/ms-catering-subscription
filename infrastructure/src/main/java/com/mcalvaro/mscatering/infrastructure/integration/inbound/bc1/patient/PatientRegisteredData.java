package com.mcalvaro.mscatering.infrastructure.integration.inbound.bc1.patient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

/** The minimal portion of BC1 event data consumed by BC3. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PatientRegisteredData(@JsonProperty("idPaciente") UUID patientId) {
}
