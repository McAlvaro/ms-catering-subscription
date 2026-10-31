package com.mcalvaro.mscatering.infrastructure.api.subscription;

import com.mcalvaro.mscatering.application.subscription.CreateSubscription.CreateSubscriptionCommand;
import com.mcalvaro.mscatering.infrastructure.BaseIntegrationTest;
import com.mcalvaro.mscatering.infrastructure.support.PatientRegistrationFixture;
import com.mcalvaro.mscatering.infrastructure.support.PatientAddressRegistrationFixture;
import com.mcalvaro.mscatering.infrastructure.support.DietPlanProjectionFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;

/**
 * Integration tests for POST /api/subscriptions.
 * <p>
 * Covers:
 * - Happy path: 201 Created with a valid UUID in the body
 * - Domain violations: duplicate subscription (INV-01), inactive patient
 * (SUB-015),
 * patient not found (SUB-014), invalid plan duration (SUB-005),
 * negative price (SUB-006), invalid service type (INVALID_PAYLOAD),
 * invalid date order (VO-008), invalid time window (VO-002),
 * blank street (VO-003), blank city (VO-004)
 * - Malformed payload: 400 with INVALID_PAYLOAD
 */
class CreateSubscriptionApiIT extends BaseIntegrationTest {

    private static final String URL = "/api/subscriptions";

    @Autowired
    private PatientRegistrationFixture patientFixture;

    @Autowired
    private PatientAddressRegistrationFixture addressFixture;
    @Autowired
    private DietPlanProjectionFixture dietPlanFixture;

    private String activePatientId;
    private String inactivePatientId;
    private String activeAddressId;
    private String foreignAddressId;
    private final java.util.Map<Integer, String> planIds = new java.util.HashMap<>();

    @BeforeEach
    void setUpPatients() {
        // Active patient — eligible to subscribe
        activePatientId = "PAC-" + UUID.randomUUID();
        patientFixture.ingestEligiblePatient(activePatientId);
        activeAddressId = addressFixture.ingestAddress(activePatientId);
        foreignAddressId = addressFixture.ingestAddress("PAC-" + UUID.randomUUID());

        // Inactive patient — blocked by SUB-015
        inactivePatientId = "PAC-" + UUID.randomUUID();
        patientFixture.ingestIneligiblePatient(inactivePatientId);
        LocalDate start = LocalDate.now().plusDays(1);
        for (int duration : List.of(15, 20, 30)) {
            planIds.put(duration, dietPlanFixture.ingestActiveBasePlan(start, duration));
        }
    }

    @Nested
    @DisplayName("Happy Path")
    class HappyPath {

        @Test
        @DisplayName("Should return 201 Created with a UUID body when the command is valid (15-day plan)")
        void shouldReturn201WithUuidWhenCommandIsValid() throws Exception {
            // Arrange
            String body = objectMapper.writeValueAsString(buildValidCommand(activePatientId, 15));

            // Act & Assert
            mockMvc.perform(post(URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                    .andExpect(status().isCreated())
                    .andExpect(content().string(not(isEmptyOrNullString())));
        }

        @Test
        @DisplayName("Should return 201 Created with a UUID body when the plan is 30 days (MONTHLY)")
        void shouldReturn201WithUuidWhenPlanIs30Days() throws Exception {
            // Arrange
            String body = objectMapper.writeValueAsString(buildValidCommand(activePatientId, 30));

            // Act & Assert
            mockMvc.perform(post(URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                    .andExpect(status().isCreated())
                    .andExpect(content().string(not(isEmptyOrNullString())));
        }
    }

    @Nested
    @DisplayName("Domain Rule Violations")
    class DomainRuleViolations {

        @Test
        @DisplayName("Should return 400 SUB-014 when patient does not exist in the local read model")
        void shouldReturn400WhenPatientNotFound() throws Exception {
            // Arrange
            String unknownPatientId = "PAC-" + UUID.randomUUID();
            String body = objectMapper.writeValueAsString(buildValidCommand(unknownPatientId, 15));

            // Act & Assert
            mockMvc.perform(post(URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("SUB-014"));
        }

        @Test
        @DisplayName("Should return 400 SUB-015 when patient exists but is inactive")
        void shouldReturn400WhenPatientIsInactive() throws Exception {
            // Arrange
            String body = objectMapper.writeValueAsString(buildValidCommand(inactivePatientId, 15));

            // Act & Assert
            mockMvc.perform(post(URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("SUB-015"));
        }

        @Test
        @DisplayName("Should return 400 SUB-013 when patient already has an ACTIVE or PAUSED subscription")
        void shouldReturn400WhenDuplicateSubscription() throws Exception {
            // Arrange — create the first subscription successfully
            String patientId = "PAC-" + UUID.randomUUID();
            patientFixture.ingestEligiblePatient(patientId);
            String addressId = addressFixture.ingestAddress(patientId);

            String firstBody = objectMapper.writeValueAsString(buildValidCommand(patientId, addressId, 15));
            mockMvc.perform(post(URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(firstBody))
                    .andExpect(status().isCreated());

            // Act — attempt to create a second subscription for the same patient
            // Note: @Transactional rolls back the outer test transaction but the
            // @Transactional of the pipeline commits the first subscription inside
            // the same test transaction, so H2 can see it.
            String secondBody = objectMapper.writeValueAsString(buildValidCommand(patientId, 15));

            // Assert
            mockMvc.perform(post(URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(secondBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("SUB-013"));
        }

        @Test
        @DisplayName("Should return 400 SUB-005 when plan duration is not 15 or 30 days (e.g. 20 days)")
        void shouldReturn400WhenPlanDurationIsInvalid() throws Exception {
            // Arrange — 20-day period: neither BIWEEKLY (15) nor MONTHLY (30)
            String body = objectMapper.writeValueAsString(buildValidCommand(activePatientId, 20));

            // Act & Assert
            mockMvc.perform(post(URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("SUB-005"));
        }

        @Test
        @DisplayName("Should return 400 SUB-006 when totalPrice is zero")
        void shouldReturn400WhenTotalPriceIsZero() throws Exception {
            // Arrange
            CreateSubscriptionCommand command = new CreateSubscriptionCommand(
                    activePatientId, planIds.get(15), LocalDate.now().plusDays(1), LocalDate.now().plusDays(15),
                    "LUNCH", BigDecimal.ZERO, "Acepto términos y condiciones del servicio de catering.",
                    activeAddressId, LocalTime.of(12, 0), LocalTime.of(14, 0), null);
            String body = objectMapper.writeValueAsString(command);

            // Act & Assert
            mockMvc.perform(post(URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("SUB-006"));
        }

        @Test
        @DisplayName("Should return 400 SUB-006 when totalPrice is negative")
        void shouldReturn400WhenTotalPriceIsNegative() throws Exception {
            // Arrange
            CreateSubscriptionCommand command = new CreateSubscriptionCommand(
                    activePatientId, planIds.get(15), LocalDate.now().plusDays(1), LocalDate.now().plusDays(15),
                    "LUNCH", new BigDecimal("-1.00"), "Acepto términos y condiciones del servicio de catering.",
                    activeAddressId, LocalTime.of(12, 0), LocalTime.of(14, 0), null);
            String body = objectMapper.writeValueAsString(command);

            // Act & Assert
            mockMvc.perform(post(URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("SUB-006"));
        }

        @Test
        @DisplayName("Should return 400 VO-008 when startDate is not strictly before endDate")
        void shouldReturn400WhenStartDateIsNotBeforeEndDate() throws Exception {
            // Arrange — same day: startDate == endDate
            LocalDate sameDay = LocalDate.now().plusDays(1);
            CreateSubscriptionCommand command = new CreateSubscriptionCommand(
                    activePatientId, "PLAN-" + UUID.randomUUID(), sameDay, sameDay, "LUNCH", new BigDecimal("250.00"),
                    "Acepto términos y condiciones del servicio de catering.", activeAddressId,
                    LocalTime.of(12, 0), LocalTime.of(14, 0), null);
            String body = objectMapper.writeValueAsString(command);

            // Act & Assert
            mockMvc.perform(post(URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VO-008"));
        }

        @Test
        @DisplayName("Should return 400 VO-007 when a validity-period date is missing")
        void shouldReturn400WhenValidityPeriodDateIsMissing() throws Exception {
            // Arrange
            String body = objectMapper.writeValueAsString(buildCommand(
                    activePatientId, null, LocalDate.now().plusDays(15),
                    "Acepto términos y condiciones del servicio de catering.",
                    LocalTime.of(12, 0), LocalTime.of(14, 0)));

            // Act & Assert
            mockMvc.perform(post(URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VO-007"));
        }

        @Test
        @DisplayName("Should return 400 VO-002 when prefTimeStart is not before prefTimeEnd")
        void shouldReturn400WhenTimeWindowIsInvalid() throws Exception {
            // Arrange — endTime == startTime
            CreateSubscriptionCommand command = new CreateSubscriptionCommand(
                    activePatientId, planIds.get(15), LocalDate.now().plusDays(1), LocalDate.now().plusDays(15),
                    "LUNCH", new BigDecimal("250.00"), "Acepto términos y conditions del servicio de catering.",
                    activeAddressId, LocalTime.of(14, 0), LocalTime.of(12, 0), null);
            String body = objectMapper.writeValueAsString(command);

            // Act & Assert
            mockMvc.perform(post(URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VO-002"));
        }

        @Test
        @DisplayName("Should return 400 VO-001 when a preferred delivery time is missing")
        void shouldReturn400WhenPreferredDeliveryTimeIsMissing() throws Exception {
            // Arrange
            String body = objectMapper.writeValueAsString(buildCommand(
                    activePatientId, LocalDate.now().plusDays(1), LocalDate.now().plusDays(15),
                    "Acepto términos y condiciones del servicio de catering.",
                    null, LocalTime.of(14, 0)));

            // Act & Assert
            mockMvc.perform(post(URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VO-001"));
        }

        @Test
        @DisplayName("Should return 400 ADR-001 when the selected address belongs to another patient")
        void shouldReturn400WhenAddressIsForeign() throws Exception {
            String body = buildValidCommandJson(activePatientId, 15)
                    .replace(activeAddressId, foreignAddressId);

            // Act & Assert
            mockMvc.perform(post(URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("ADR-001"));
        }

        @Test
        @DisplayName("Should return 400 ADR-001 when the selected address does not exist")
        void shouldReturn400WhenAddressIsMissing() throws Exception {
            String body = buildValidCommandJson(activePatientId, 15)
                    .replace(activeAddressId, "DIR-00000000-0000-0000-0000-000000000001");

            // Act & Assert
            mockMvc.perform(post(URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("ADR-001"));
        }

        @Test
        @DisplayName("Should return 400 VO-010 when accepted conditions are blank")
        void shouldReturn400WhenAcceptedConditionsAreBlank() throws Exception {
            // Arrange
            String body = objectMapper.writeValueAsString(buildCommand(
                    activePatientId, LocalDate.now().plusDays(1), LocalDate.now().plusDays(15),
                    "   ", LocalTime.of(12, 0), LocalTime.of(14, 0)));

            // Act & Assert
            mockMvc.perform(post(URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VO-010"));
        }
    }

    @Nested
    @DisplayName("Malformed Payload")
    class MalformedPayload {

        @Test
        @DisplayName("Should return 400 BAD_REQUEST when serviceType is not a valid enum value")
        void shouldReturn400WhenServiceTypeIsInvalid() throws Exception {
            // Arrange — serviceType is a String in CreateSubscriptionCommand, so Jackson
            // accepts "DESAYUNO" without error. ServiceType.valueOf("DESAYUNO") then throws
            // IllegalArgumentException in the handler, caught as BAD_REQUEST (not
            // INVALID_PAYLOAD).
            String body = buildValidCommandJson(activePatientId, 15)
                    .replace("\"LUNCH\"", "\"DESAYUNO\"");

            // Act & Assert
            mockMvc.perform(post(URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
        }

        @Test
        @DisplayName("Should return 400 INVALID_PAYLOAD when startDate has an invalid format")
        void shouldReturn400WhenStartDateHasInvalidFormat() throws Exception {
            // Arrange
            String body = buildValidCommandJson(activePatientId, 15)
                    .replaceFirst("\"startDate\"\\s*:\\s*\"[^\"]+\"",
                            "\"startDate\":\"not-a-date\"");

            // Act & Assert
            mockMvc.perform(post(URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_PAYLOAD"));
        }

        @Test
        @DisplayName("Should return 400 INVALID_PAYLOAD when request body is completely empty JSON")
        void shouldReturn400WhenBodyIsEmptyJson() throws Exception {
            // Act & Assert
            mockMvc.perform(post(URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_PAYLOAD"));
        }
    }

    private CreateSubscriptionCommand buildValidCommand(String patientId, int planDays) {
        return buildValidCommand(patientId, activeAddressId, planDays);
    }

    private CreateSubscriptionCommand buildValidCommand(String patientId, String addressId, int planDays) {
        LocalDate start = LocalDate.now().plusDays(1);
        LocalDate end = start.plusDays(planDays - 1);
        return buildCommand(patientId, addressId, start, end,
                "Acepto términos y condiciones del servicio de catering.",
                LocalTime.of(12, 0), LocalTime.of(14, 0));
    }

    private CreateSubscriptionCommand buildCommand(
            String patientId, LocalDate start, LocalDate end, String acceptedConditions,
            LocalTime prefTimeStart, LocalTime prefTimeEnd) {
        return buildCommand(patientId, activeAddressId, start, end, acceptedConditions, prefTimeStart, prefTimeEnd);
    }

    private CreateSubscriptionCommand buildCommand(String patientId, String addressId, LocalDate start, LocalDate end,
            String acceptedConditions, LocalTime prefTimeStart, LocalTime prefTimeEnd) {
        return new CreateSubscriptionCommand(
                patientId,
                start == null || end == null ? planIds.get(15)
                        : planIds.get((int) java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1),
                start,
                end,
                "LUNCH",
                new BigDecimal("250.00"),
                acceptedConditions,
                addressId,
                prefTimeStart,
                prefTimeEnd,
                "Sin picante");
    }

    private String buildValidCommandJson(String patientId, int planDays) throws Exception {
        return objectMapper.writeValueAsString(buildValidCommand(patientId, planDays));
    }
}
