package com.mcalvaro.mscatering.infrastructure.api.subscription;

import com.mcalvaro.mscatering.domain.subscription.*;
import com.mcalvaro.mscatering.domain.subscription.enums.ServiceType;
import com.mcalvaro.mscatering.domain.subscription.vo.*;
import com.mcalvaro.mscatering.infrastructure.BaseIntegrationTest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CancelSubscriptionApiIT extends BaseIntegrationTest {
    private static final String URL = "/api/subscriptions/{id}";
    @Autowired
    private ISubscriptionRepository subscriptionRepository;
    private UUID subscriptionId;

    @BeforeEach
    void setUp() {
        subscriptionId = UUID.randomUUID();
        subscriptionRepository.save(buildValidCommand());
    }

    @Nested
    @DisplayName("Happy Path")
    class HappyPath {
        @Test
        @DisplayName("Should return 200 OK when subscription is cancelled")
        void shouldReturn200WhenSubscriptionExists() throws Exception {// Arrange
            // Act & Assert
            mockMvc.perform(delete(URL, subscriptionId).param("reason", "Patient request")).andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Domain Rule Violations")
    class DomainRuleViolations {
        @Test
        @DisplayName("Should return 404 NOT_FOUND when subscription does not exist")
        void shouldReturn404WhenSubscriptionDoesNotExist() throws Exception {// Arrange
            // Act & Assert
            mockMvc.perform(delete(URL, UUID.randomUUID()).param("reason", "Patient request"))
                    .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("NOT_FOUND"));
        }
    }

    @Nested
    @DisplayName("Malformed Payload")
    class MalformedPayload {
        @Test
        @DisplayName("Should return 500 INTERNAL_SERVER_ERROR when required reason is absent")
        void shouldReturn500WhenReasonIsMissing() throws Exception {// Arrange
            // Act & Assert
            mockMvc.perform(delete(URL, subscriptionId)).andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"));
        }
    }

    private Subscription buildValidCommand() {
        LocalDate start = LocalDate.now().plusDays(10);
        return Subscription.create(subscriptionId, UUID.randomUUID(), UUID.randomUUID(),
                new ServiceContract(UUID.randomUUID(), new ValidityPeriod(start, start.plusDays(14)), ServiceType.FULL,
                        new BigDecimal("250"), "Terms", Instant.now()),
                new DeliveryPreferences(new DeliveryAddress("Main", "1", "Lima", null, 0, 0, "1"),
                        new TimeWindow(LocalTime.NOON, LocalTime.of(14, 0)), null),
                1);
    }

    private Subscription buildValidCommand(UUID ignored, int planDays) {
        return buildValidCommand();
    }

    private String buildValidCommandJson() throws Exception {
        return objectMapper.writeValueAsString(buildValidCommand());
    }
}
