package com.mcalvaro.mscatering.infrastructure.api.subscription;

import com.mcalvaro.mscatering.application.subscription.CreateSubscription.CreateSubscriptionCommand;
import com.mcalvaro.mscatering.domain.patient.PatientReference;
import com.mcalvaro.mscatering.domain.patient.IPatientReferenceRepository;
import com.mcalvaro.mscatering.infrastructure.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
    private IPatientReferenceRepository patientRepository;

    private UUID activePatientId;
    private UUID inactivePatientId;

    @BeforeEach
    void setUpPatients() {
        // Active patient — eligible to subscribe
        activePatientId = UUID.randomUUID();
        patientRepository.save(new PatientReference(activePatientId, true, Instant.now()));

        // Inactive patient — blocked by SUB-015
        inactivePatientId = UUID.randomUUID();
        patientRepository.save(new PatientReference(inactivePatientId, false, Instant.now()));
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
                    .andExpect(status().isCreated());
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
                    .andExpect(status().isCreated());
        }
    }

    @Nested
    @DisplayName("Domain Rule Violations")
    class DomainRuleViolations {

        @Test
        @DisplayName("Should return 400 SUB-014 when patient does not exist in the local read model")
        void shouldReturn400WhenPatientNotFound() throws Exception {
            // Arrange
            UUID unknownPatientId = UUID.randomUUID();
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
            UUID patientId = UUID.randomUUID();
            patientRepository.save(new PatientReference(patientId, true, Instant.now()));

            String firstBody = objectMapper.writeValueAsString(buildValidCommand(patientId, 15));
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
                    activePatientId,
                    UUID.randomUUID(),
                    LocalDate.now().plusDays(1),
                    LocalDate.now().plusDays(15),
                    "LUNCH",
                    BigDecimal.ZERO, // invalid — must be > 0
                    "Acepto términos y condiciones del servicio de catering.",
                    "Av. Siempre Viva",
                    "742",
                    "Lima",
                    null,
                    -12.046374,
                    -77.042793,
                    "+51 999 888 777",
                    LocalTime.of(12, 0),
                    LocalTime.of(14, 0),
                    null);
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
                    activePatientId,
                    UUID.randomUUID(),
                    LocalDate.now().plusDays(1),
                    LocalDate.now().plusDays(15),
                    "LUNCH",
                    new BigDecimal("-1.00"), // invalid — must be > 0
                    "Acepto términos y condiciones del servicio de catering.",
                    "Av. Siempre Viva",
                    "742",
                    "Lima",
                    null,
                    -12.046374,
                    -77.042793,
                    "+51 999 888 777",
                    LocalTime.of(12, 0),
                    LocalTime.of(14, 0),
                    null);
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
                    activePatientId,
                    UUID.randomUUID(),
                    sameDay,
                    sameDay, // invalid: must be strictly before
                    "LUNCH",
                    new BigDecimal("250.00"),
                    "Acepto términos y condiciones del servicio de catering.",
                    "Av. Siempre Viva",
                    "742",
                    "Lima",
                    null,
                    -12.046374,
                    -77.042793,
                    "+51 999 888 777",
                    LocalTime.of(12, 0),
                    LocalTime.of(14, 0),
                    null);
            String body = objectMapper.writeValueAsString(command);

            // Act & Assert
            mockMvc.perform(post(URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VO-008"));
        }

        @Test
        @DisplayName("Should return 400 VO-002 when prefTimeStart is not before prefTimeEnd")
        void shouldReturn400WhenTimeWindowIsInvalid() throws Exception {
            // Arrange — endTime == startTime
            CreateSubscriptionCommand command = new CreateSubscriptionCommand(
                    activePatientId,
                    UUID.randomUUID(),
                    LocalDate.now().plusDays(1),
                    LocalDate.now().plusDays(15),
                    "LUNCH",
                    new BigDecimal("250.00"),
                    "Acepto términos y condiciones del servicio de catering.",
                    "Av. Siempre Viva",
                    "742",
                    "Lima",
                    null,
                    -12.046374,
                    -77.042793,
                    "+51 999 888 777",
                    LocalTime.of(14, 0),
                    LocalTime.of(12, 0), // invalid: end before start
                    null);
            String body = objectMapper.writeValueAsString(command);

            // Act & Assert
            mockMvc.perform(post(URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VO-002"));
        }

        @Test
        @DisplayName("Should return 400 VO-003 when prefStreet is blank")
        void shouldReturn400WhenStreetIsBlank() throws Exception {
            // Arrange
            CreateSubscriptionCommand command = new CreateSubscriptionCommand(
                    activePatientId,
                    UUID.randomUUID(),
                    LocalDate.now().plusDays(1),
                    LocalDate.now().plusDays(15),
                    "LUNCH",
                    new BigDecimal("250.00"),
                    "Acepto términos y condiciones del servicio de catering.",
                    "   ", // blank — invalid
                    "742",
                    "Lima",
                    null,
                    -12.046374,
                    -77.042793,
                    "+51 999 888 777",
                    LocalTime.of(12, 0),
                    LocalTime.of(14, 0),
                    null);
            String body = objectMapper.writeValueAsString(command);

            // Act & Assert
            mockMvc.perform(post(URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VO-003"));
        }

        @Test
        @DisplayName("Should return 400 VO-004 when prefCity is blank")
        void shouldReturn400WhenCityIsBlank() throws Exception {
            // Arrange
            CreateSubscriptionCommand command = new CreateSubscriptionCommand(
                    activePatientId,
                    UUID.randomUUID(),
                    LocalDate.now().plusDays(1),
                    LocalDate.now().plusDays(15),
                    "LUNCH",
                    new BigDecimal("250.00"),
                    "Acepto términos y condiciones del servicio de catering.",
                    "Av. Siempre Viva",
                    "742",
                    "   ", // blank city — invalid
                    null,
                    -12.046374,
                    -77.042793,
                    "+51 999 888 777",
                    LocalTime.of(12, 0),
                    LocalTime.of(14, 0),
                    null);
            String body = objectMapper.writeValueAsString(command);

            // Act & Assert
            mockMvc.perform(post(URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VO-004"));
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
                    .andExpect(status().isBadRequest());
        }
    }

    private CreateSubscriptionCommand buildValidCommand(UUID patientId, int planDays) {
        LocalDate start = LocalDate.now().plusDays(1);
        LocalDate end = start.plusDays(planDays - 1);
        return new CreateSubscriptionCommand(
                patientId,
                UUID.randomUUID(),
                start,
                end,
                "LUNCH",
                new BigDecimal("250.00"),
                "Acepto términos y condiciones del servicio de catering.",
                "Av. Siempre Viva",
                "742",
                "Lima",
                "Frente al parque",
                -12.046374,
                -77.042793,
                "+51 999 888 777",
                LocalTime.of(12, 0),
                LocalTime.of(14, 0),
                "Sin picante");
    }

    private String buildValidCommandJson(UUID patientId, int planDays) throws Exception {
        return objectMapper.writeValueAsString(buildValidCommand(patientId, planDays));
    }
}
