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

class CompleteSubscriptionApiIT extends BaseIntegrationTest {
    private static final String URL = "/api/subscriptions/{id}/complete";
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
        @DisplayName("Should return 200 OK when contract period has ended")
        void shouldReturn200WhenPeriodHasEnded() throws Exception {// Arrange
            // Act & Assert
            mockMvc.perform(post(URL, subscriptionId)).andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Domain Rule Violations")
    class DomainRuleViolations {
        @Test
        @DisplayName("Should return 400 SUB-008 when contract period has not ended")
        void shouldReturn400WhenPeriodHasNotEnded() throws Exception {// Arrange
            UUID activeId = UUID.randomUUID();
            subscriptionRepository.save(buildFutureSubscription(activeId));
            // Act & Assert
            mockMvc.perform(post(URL, activeId)).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("SUB-008"));
        }

        @Test
        @DisplayName("Should return 400 SUB-003 when subscription is paused")
        void shouldReturn400WhenSubscriptionIsPaused() throws Exception {// Arrange
            UUID pausedId = UUID.randomUUID();
            Subscription s = buildValidCommand(pausedId);
            s.pause(new PauseRange(LocalDate.now().plusDays(3), LocalDate.now().plusDays(5)), "Holiday");
            subscriptionRepository.save(s);
            // Act & Assert
            mockMvc.perform(post(URL, pausedId)).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("SUB-003"));
        }
    }

    @Nested
    @DisplayName("Malformed Payload")
    class MalformedPayload {
        @Test
        @DisplayName("Should return 500 INTERNAL_SERVER_ERROR when subscription ID is malformed")
        void shouldReturn500WhenIdIsMalformed() throws Exception {// Arrange
            // Act & Assert
            mockMvc.perform(post(URL, "invalid")).andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"));
        }
    }

    private Subscription buildValidCommand() {
        return buildValidCommand(subscriptionId);
    }

    private Subscription buildValidCommand(UUID id) {
        LocalDate end = LocalDate.now().minusDays(1);
        return Subscription.create(id, UUID.randomUUID(), UUID.randomUUID(),
                new ServiceContract(UUID.randomUUID(), new ValidityPeriod(end.minusDays(14), end), ServiceType.FULL,
                        new BigDecimal("250"), "Terms", Instant.now()),
                new DeliveryPreferences(new DeliveryAddress("Main", "1", "Lima", null, 0, 0, "1"),
                        new TimeWindow(LocalTime.NOON, LocalTime.of(14, 0)), null),
                1);
    }

    private Subscription buildValidCommand(UUID ignored, int planDays) {
        return buildValidCommand();
    }

    private Subscription buildFutureSubscription(UUID id) {
        LocalDate start = LocalDate.now().plusDays(10);
        return Subscription.create(id, UUID.randomUUID(), UUID.randomUUID(),
                new ServiceContract(UUID.randomUUID(), new ValidityPeriod(start, start.plusDays(14)), ServiceType.FULL,
                        new BigDecimal("250"), "Terms", Instant.now()),
                new DeliveryPreferences(new DeliveryAddress("Main", "1", "Lima", null, 0, 0, "1"),
                        new TimeWindow(LocalTime.NOON, LocalTime.of(14, 0)), null),
                1);
    }

    private String buildValidCommandJson() throws Exception {
        return objectMapper.writeValueAsString(buildValidCommand());
    }
}
