package com.mcalvaro.mscatering.integration.contracts;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ConsolidatedCalendarClosedIntegrationEvent(UUID eventId, Instant occurredOn, String correlationId,
        Data data)
        implements IntegrationEvent {
    public static final String EVENT_NAME = "bc3.CalendarioConsolidadoCerrado";
    public static final String LOGICAL_DESTINATION = "bc3.calendario.consolidado.cerrado.v1";

    public ConsolidatedCalendarClosedIntegrationEvent {
        ContractAssertions.envelope(eventId, occurredOn, correlationId, data);
    }

    @Override
    public String eventName() {
        return EVENT_NAME;
    }

    @Override
    public String logicalDestination() {
        return LOGICAL_DESTINATION;
    }

    public record Data(
            @JsonProperty("idCalendario") String calendarId,
            @JsonProperty("fechaEntrega") LocalDate deliveryDate,
            @JsonProperty("fechaCierre") Instant closedOn,
            @JsonProperty("totalEntregas") int totalDeliveries,
            @JsonProperty("entregas") List<Delivery> deliveries) {
        public Data {
            ContractAssertions.text(calendarId, "calendarId");
            ContractAssertions.date(deliveryDate, "deliveryDate");
            ContractAssertions.required(closedOn, "closedOn");
            deliveries = ContractAssertions.nonEmptyList(deliveries, "deliveries");
            if (totalDeliveries != deliveries.size())
                throw new IllegalArgumentException("totalDeliveries must equal deliveries size");
        }
    }

    public record Delivery(
            @JsonProperty("idCalendarioDia") String calendarDayId,
            @JsonProperty("idSuscripcion") String subscriptionId,
            @JsonProperty("idPaciente") String patientId,
            @JsonProperty("nombrePaciente") String patientName,
            @JsonProperty("idPlan") String planId,
            @JsonProperty("numDiaPlan") int planDayNumber,
            @JsonProperty("direccion") Address address,
            @JsonProperty("recetas") List<Recipe> recipes) {
        public Delivery {
            ContractAssertions.text(calendarDayId, "calendarDayId");
            ContractAssertions.text(subscriptionId, "subscriptionId");
            ContractAssertions.text(patientId, "patientId");
            ContractAssertions.text(patientName, "patientName");
            ContractAssertions.text(planId, "planId");
            ContractAssertions.positive(planDayNumber, "planDayNumber");
            ContractAssertions.required(address, "address");
            recipes = ContractAssertions.nonEmptyList(recipes, "recipes");
        }
    }

    public record Address(
            @JsonProperty("idDireccion") String addressId,
            @JsonProperty("alias") String alias,
            @JsonProperty("direccion") String addressText,
            @JsonProperty("referencia") String reference,
            @JsonProperty("ciudad") String city,
            @JsonProperty("latitud") double latitude,
            @JsonProperty("longitud") double longitude) {
        public Address {
            ContractAssertions.text(addressId, "addressId");
            ContractAssertions.text(alias, "alias");
            ContractAssertions.text(addressText, "addressText");
            ContractAssertions.text(reference, "reference");
            ContractAssertions.text(city, "city");
            ContractAssertions.coordinates(latitude, longitude);
        }
    }

    public record Recipe(
            @JsonProperty("idReceta") String recipeId,
            @JsonProperty("nombre") String name,
            @JsonProperty("hora") String time,
            @JsonProperty("porcionGramos") BigDecimal portionGrams) {
        public Recipe {
            ContractAssertions.text(recipeId, "recipeId");
            ContractAssertions.text(name, "name");
            if (time == null || !time.matches("^([01][0-9]|2[0-3]):[0-5][0-9]$")) {
                throw new IllegalArgumentException("time must use HH:mm");
            }
            ContractAssertions.positive(portionGrams, "portionGrams");
        }
    }
}
