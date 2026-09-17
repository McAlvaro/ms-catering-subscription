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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ReactivateSubscriptionApiIT extends BaseIntegrationTest {
    private static final String URL = "/api/subscriptions/{id}/reactivate";
    @Autowired
    private ISubscriptionRepository subscriptionRepository;
    private UUID subscriptionId;

    @BeforeEach
    void setUp() {
        subscriptionId = UUID.randomUUID();
        Subscription subscription = buildValidCommand();
        subscription.pause(new PauseRange(LocalDate.now().plusDays(3), LocalDate.now().plusDays(5)), "Holiday");
        subscriptionRepository.save(subscription);
    }

    @Nested
    @DisplayName("Happy Path")
    class HappyPath {
        @Test
        @DisplayName("Should return 200 OK when a paused subscription is reactivated")
        void shouldReturn200WhenSubscriptionIsPaused() throws Exception {
            // Arrange
            LocalDate date = LocalDate.now().plusDays(3);
            // Act & Assert
            mockMvc.perform(post(URL, subscriptionId).param("reactivationDate", date.toString()))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Domain Rule Violations")
    class DomainRuleViolations {
        @Test
        @DisplayName("Should return 400 SUB-004 when subscription is active")
        void shouldReturn400WhenSubscriptionIsActive() throws Exception {
            // Arrange
            UUID activeId = UUID.randomUUID();
            subscriptionRepository.save(buildValidCommand(activeId));
            // Act & Assert
            mockMvc.perform(post(URL, activeId).param("reactivationDate", LocalDate.now().plusDays(3).toString()))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("SUB-004"));
        }
    }

    @Nested
    @DisplayName("Malformed Payload")
    class MalformedPayload {
        @Test
        @DisplayName("Should return 500 INTERNAL_SERVER_ERROR when reactivationDate is malformed")
        void shouldReturn500WhenReactivationDateIsMalformed() throws Exception {
            // Arrange
            // Act & Assert
            mockMvc.perform(post(URL, subscriptionId).param("reactivationDate", "invalid"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"));
        }
    }

    private Subscription buildValidCommand() {
        return buildValidCommand(subscriptionId);
    }

    private Subscription buildValidCommand(UUID id) {
        LocalDate start = LocalDate.now().plusDays(10);
        return Subscription.create(id, UUID.randomUUID(), UUID.randomUUID(),
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
