package com.mcalvaro.mscatering.infrastructure.api.subscription;

import com.mcalvaro.mscatering.application.subscription.PauseSubscription.PauseSubscriptionCommand;
import com.mcalvaro.mscatering.domain.subscription.ISubscriptionRepository;
import com.mcalvaro.mscatering.domain.subscription.Subscription;
import com.mcalvaro.mscatering.domain.subscription.enums.ServiceType;
import com.mcalvaro.mscatering.domain.subscription.vo.*;
import com.mcalvaro.mscatering.infrastructure.BaseIntegrationTest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PauseSubscriptionApiIT extends BaseIntegrationTest {
    private static final String URL = "/api/subscriptions/{id}/pause";
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
        @DisplayName("Should return 200 OK when an active subscription is paused")
        void shouldReturn200WhenPauseIsValid() throws Exception {
            // Arrange
            String body = buildValidCommandJson(LocalDate.now().plusDays(3), LocalDate.now().plusDays(5));
            // Act & Assert
            mockMvc.perform(post(URL, subscriptionId).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Domain Rule Violations")
    class DomainRuleViolations {
        @Test
        @DisplayName("Should return 400 SUB-001 when pause starts within 48 hours")
        void shouldReturn400WhenPauseStartsTooSoon() throws Exception {
            // Arrange
            String body = buildValidCommandJson(LocalDate.now().plusDays(1), LocalDate.now().plusDays(3));
            // Act & Assert
            mockMvc.perform(post(URL, subscriptionId).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("SUB-001"));
        }

        @Test
        @DisplayName("Should return 400 VO-014 when pause dates are not ordered")
        void shouldReturn400WhenPauseDatesAreNotOrdered() throws Exception {
            // Arrange
            LocalDate date = LocalDate.now().plusDays(3);
            String body = buildValidCommandJson(date, date);
            // Act & Assert
            mockMvc.perform(post(URL, subscriptionId).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VO-014"));
        }

        @Test
        @DisplayName("Should return 400 SUB-003 when subscription is not active")
        void shouldReturn400WhenSubscriptionIsAlreadyPaused() throws Exception {
            // Arrange
            Subscription pausedSubscription = subscriptionRepository.findById(subscriptionId).orElseThrow();
            pausedSubscription.pause(new PauseRange(LocalDate.now().plusDays(3), LocalDate.now().plusDays(5)),
                    "Holiday");
            subscriptionRepository.save(pausedSubscription);
            String body = buildValidCommandJson(LocalDate.now().plusDays(6), LocalDate.now().plusDays(8));
            // Act & Assert
            mockMvc.perform(post(URL, subscriptionId).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("SUB-003"));
        }
    }

    @Nested
    @DisplayName("Malformed Payload")
    class MalformedPayload {
        @Test
        @DisplayName("Should return 400 INVALID_PAYLOAD when startDate has an invalid format")
        void shouldReturn400WhenStartDateIsInvalid() throws Exception {
            // Arrange
            String body = buildValidCommandJson(LocalDate.now().plusDays(3), LocalDate.now().plusDays(5))
                    .replaceFirst("\"startDate\"\\s*:\\s*\"[^\"]+\"", "\"startDate\":\"invalid\"");
            // Act & Assert
            mockMvc.perform(post(URL, subscriptionId).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_PAYLOAD"));
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

    private String buildValidCommandJson(LocalDate start, LocalDate end) throws Exception {
        return objectMapper.writeValueAsString(new PauseSubscriptionCommand(subscriptionId, start, end, "Holiday"));
    }
}
