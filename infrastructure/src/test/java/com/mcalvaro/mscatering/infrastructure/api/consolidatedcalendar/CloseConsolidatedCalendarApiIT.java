package com.mcalvaro.mscatering.infrastructure.api.consolidatedcalendar;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mcalvaro.mscatering.domain.subscription.ISubscriptionRepository;
import com.mcalvaro.mscatering.domain.subscription.Subscription;
import com.mcalvaro.mscatering.domain.subscription.enums.ServiceType;
import com.mcalvaro.mscatering.domain.subscription.vo.DeliveryAddress;
import com.mcalvaro.mscatering.domain.subscription.vo.DeliveryPreferences;
import com.mcalvaro.mscatering.domain.subscription.vo.ServiceContract;
import com.mcalvaro.mscatering.domain.subscription.vo.TimeWindow;
import com.mcalvaro.mscatering.domain.subscription.vo.ValidityPeriod;
import com.mcalvaro.mscatering.infrastructure.BaseIntegrationTest;
import com.mcalvaro.mscatering.infrastructure.api.consolidatedcalendar.dto.CloseConsolidatedCalendarRequest;
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

import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Integration tests for POST /api/consolidated-calendars/close. */
class CloseConsolidatedCalendarApiIT extends BaseIntegrationTest {

    private static final String URL = "/api/consolidated-calendars/close";

    @Autowired
    private ISubscriptionRepository subscriptionRepository;

    private UUID subscriptionId;
    private UUID patientId;
    private UUID dietPlanId;
    private LocalDate deliveryDate;

    @BeforeEach
    void setUpSubscription() {
        subscriptionId = UUID.randomUUID();
        patientId = UUID.randomUUID();
        dietPlanId = UUID.randomUUID();
        deliveryDate = LocalDate.now().plusDays(10);
        subscriptionRepository.save(buildSubscription());
    }

    @Nested
    @DisplayName("Happy Path")
    class HappyPath {

        @Test
        @DisplayName("Should return 201 Created with a calendar UUID when the target date has a scheduled delivery")
        void shouldReturn201WithCalendarUuidWhenTargetDateHasScheduledDelivery() throws Exception {
            // Arrange
            String body = buildValidCommandJson(deliveryDate);

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
        @DisplayName("Should return 400 CAL-002 when the target date has no scheduled deliveries")
        void shouldReturn400WhenTargetDateHasNoScheduledDeliveries() throws Exception {
            // Arrange
            String body = buildValidCommandJson(deliveryDate.plusDays(30));

            // Act & Assert
            mockMvc.perform(post(URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("CAL-002"));
        }
    }

    @Nested
    @DisplayName("Malformed Payload")
    class MalformedPayload {

        @Test
        @DisplayName("Should return 400 INVALID_PAYLOAD when the date has an invalid format")
        void shouldReturn400WhenDateHasInvalidFormat() throws Exception {
            // Arrange
            String body = "{\"date\":\"not-a-date\",\"closedBy\":\"daily-operator\"}";

            // Act & Assert
            mockMvc.perform(post(URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_PAYLOAD"));
        }

        @Test
        @DisplayName("Should return 400 CAL-002 when the request body is empty")
        void shouldReturn400WhenRequestBodyIsEmpty() throws Exception {
            // Arrange
            String body = "{}";

            // Act & Assert
            mockMvc.perform(post(URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("CAL-002"));
        }
    }

    private Subscription buildSubscription() {
        ServiceContract contract = new ServiceContract(
                dietPlanId,
                new ValidityPeriod(deliveryDate, deliveryDate.plusDays(14)),
                ServiceType.LUNCH,
                new BigDecimal("150.00"),
                "Accepted terms",
                Instant.now());
        DeliveryPreferences preferences = new DeliveryPreferences(
                new DeliveryAddress("Main Street", "123", "Madrid", "Reception", 40.4168, -3.7038,
                        "+34123456789"),
                new TimeWindow(LocalTime.of(12, 0), LocalTime.of(14, 0)),
                "Ring the doorbell");
        return Subscription.create(subscriptionId, patientId, dietPlanId, contract, preferences, 1);
    }

    private CloseConsolidatedCalendarRequest buildValidCommand(LocalDate date) {
        return new CloseConsolidatedCalendarRequest(date, "daily-operator");
    }

    private String buildValidCommandJson(LocalDate date) throws JsonProcessingException {
        return objectMapper.writeValueAsString(buildValidCommand(date));
    }
}
