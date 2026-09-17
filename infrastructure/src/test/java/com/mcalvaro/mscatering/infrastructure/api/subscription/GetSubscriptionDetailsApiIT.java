package com.mcalvaro.mscatering.infrastructure.api.subscription;

import com.mcalvaro.mscatering.domain.subscription.ISubscriptionRepository;
import com.mcalvaro.mscatering.domain.subscription.Subscription;
import com.mcalvaro.mscatering.domain.subscription.enums.ServiceType;
import com.mcalvaro.mscatering.domain.subscription.vo.DeliveryAddress;
import com.mcalvaro.mscatering.domain.subscription.vo.DeliveryPreferences;
import com.mcalvaro.mscatering.domain.subscription.vo.ServiceContract;
import com.mcalvaro.mscatering.domain.subscription.vo.TimeWindow;
import com.mcalvaro.mscatering.domain.subscription.vo.ValidityPeriod;
import com.mcalvaro.mscatering.infrastructure.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Integration tests for GET /api/subscriptions/{id}. */
class GetSubscriptionDetailsApiIT extends BaseIntegrationTest {

    private static final String URL = "/api/subscriptions/{id}";

    @Autowired
    private ISubscriptionRepository subscriptionRepository;

    private UUID subscriptionId;
    private UUID patientId;
    private UUID dietPlanId;

    @BeforeEach
    void setUpSubscription() {
        subscriptionId = UUID.randomUUID();
        patientId = UUID.randomUUID();
        dietPlanId = UUID.randomUUID();
        subscriptionRepository.save(buildSubscription());
    }

    @Nested
    @DisplayName("Happy Path")
    class HappyPath {

        @Test
        @DisplayName("Should return 200 OK with subscription details when the subscription exists")
        void shouldReturn200WithSubscriptionDetailsWhenSubscriptionExists() throws Exception {
            // Arrange
            LocalDate expectedStartDate = LocalDate.now().plusDays(10);

            // Act & Assert
            mockMvc.perform(get(URL, subscriptionId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(subscriptionId.toString()))
                    .andExpect(jsonPath("$.patientId").value(patientId.toString()))
                    .andExpect(jsonPath("$.dietPlanId").value(dietPlanId.toString()))
                    .andExpect(jsonPath("$.status").value("ACTIVE"))
                    .andExpect(jsonPath("$.startDate").value(expectedStartDate.toString()))
                    .andExpect(jsonPath("$.endDate").value(expectedStartDate.plusDays(14).toString()))
                    .andExpect(jsonPath("$.serviceType").value("FULL"))
                    .andExpect(jsonPath("$.totalPrice").value(250.00))
                    .andExpect(jsonPath("$.deliveryDays.length()").value(15))
                    .andExpect(jsonPath("$.evaluations.length()").value(0));
        }
    }

    @Nested
    @DisplayName("Domain Rule Violations")
    class DomainRuleViolations {

        @Test
        @DisplayName("Should return 404 NOT_FOUND when the subscription does not exist")
        void shouldReturn404WhenSubscriptionDoesNotExist() throws Exception {
            // Arrange
            UUID unknownSubscriptionId = UUID.randomUUID();

            // Act & Assert
            mockMvc.perform(get(URL, unknownSubscriptionId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        }
    }

    @Nested
    @DisplayName("Malformed Payload")
    class MalformedPayload {

        @Test
        @DisplayName("Should return 500 INTERNAL_SERVER_ERROR when the subscription ID is not a UUID")
        void shouldReturn500WhenSubscriptionIdIsNotUuid() throws Exception {
            // Arrange
            String invalidSubscriptionId = "not-a-uuid";

            // Act & Assert
            mockMvc.perform(get(URL, invalidSubscriptionId))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"));
        }
    }

    private Subscription buildSubscription() {
        LocalDate startDate = LocalDate.now().plusDays(10);
        ServiceContract contract = new ServiceContract(
                UUID.randomUUID(),
                new ValidityPeriod(startDate, startDate.plusDays(14)),
                ServiceType.FULL,
                new BigDecimal("250.00"),
                "Terms accepted",
                Instant.now());
        DeliveryPreferences preferences = new DeliveryPreferences(
                new DeliveryAddress("Main Street", "10", "Madrid", "Door A", 40.4168, -3.7038, "600000000"),
                new TimeWindow(LocalTime.of(9, 0), LocalTime.of(11, 0)),
                "Leave at reception");
        return Subscription.create(subscriptionId, patientId, dietPlanId, contract, preferences, 1);
    }
}
