package com.mcalvaro.mscatering.infrastructure.api.subscription;

import com.mcalvaro.mscatering.application.subscription.PauseSubscription.PauseSubscriptionCommand;
import com.mcalvaro.mscatering.domain.core.id.AddressId;
import com.mcalvaro.mscatering.domain.core.id.DietPlanId;
import com.mcalvaro.mscatering.domain.core.id.PatientId;
import com.mcalvaro.mscatering.domain.core.id.SubscriptionId;
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
import static org.assertj.core.api.Assertions.assertThat;

class PauseSubscriptionApiIT extends BaseIntegrationTest {
    private static final String URL = "/api/subscriptions/{id}/pause";
    @Autowired
    private ISubscriptionRepository subscriptionRepository;
    private SubscriptionId subscriptionId;

    @BeforeEach
    void setUp() {
        subscriptionId = SubscriptionId.of("SUS-" + UUID.randomUUID());
        subscriptionRepository.save(buildValidCommand());
    }

    @Nested
    @DisplayName("Happy Path")
    class HappyPath {
        @Test
        @DisplayName("Should return 200 OK when an active subscription is paused")
        void shouldReturn200WhenPauseIsValid() throws Exception {
            // Arrange
            String body = buildValidCommandJson(LocalDate.now().plusDays(12), LocalDate.now().plusDays(14));
            // Act & Assert
            mockMvc.perform(post(URL, subscriptionId.value()).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should return and persist the received correlation ID when a subscription is paused")
        void shouldReturnAndPersistReceivedCorrelationIdWhenPauseIsValid() throws Exception {
            // Arrange
            String correlationId = "received-correlation-id";
            String body = buildValidCommandJson(LocalDate.now().plusDays(12), LocalDate.now().plusDays(14));

            // Act & Assert
            mockMvc.perform(post(URL, subscriptionId.value()).header("X-Correlation-Id", correlationId)
                    .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isOk())
                    .andExpect(header().string("X-Correlation-Id", correlationId));
            assertThat(subscriptionRepository.findById(subscriptionId).orElseThrow().getPauseRequests()).singleElement()
                    .extracting(request -> request.getCorrelationId()).isEqualTo(correlationId);
        }

        @Test
        @DisplayName("Should generate and return a correlation ID when the request header is blank")
        void shouldGenerateCorrelationIdWhenRequestHeaderIsBlank() throws Exception {
            // Arrange
            String body = buildValidCommandJson(LocalDate.now().plusDays(12), LocalDate.now().plusDays(14));

            // Act & Assert
            String generatedCorrelationId = mockMvc.perform(post(URL, subscriptionId.value()).header("X-Correlation-Id", "   ")
                    .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isOk())
                    .andExpect(header().exists("X-Correlation-Id"))
                    .andReturn().getResponse().getHeader("X-Correlation-Id");
            assertThat(UUID.fromString(generatedCorrelationId)).isNotNull();
            assertThat(subscriptionRepository.findById(subscriptionId).orElseThrow().getPauseRequests()).singleElement()
                    .extracting(request -> request.getCorrelationId()).isEqualTo(generatedCorrelationId);
        }

        @Test
        @DisplayName("Should generate and persist a correlation ID when the request header is absent")
        void shouldGenerateCorrelationIdWhenRequestHeaderIsAbsent() throws Exception {
            // Arrange
            String body = buildValidCommandJson(LocalDate.now().plusDays(12), LocalDate.now().plusDays(14));

            // Act & Assert
            String generatedCorrelationId = mockMvc.perform(post(URL, subscriptionId.value())
                    .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isOk())
                    .andExpect(header().exists("X-Correlation-Id"))
                    .andReturn().getResponse().getHeader("X-Correlation-Id");
            assertThat(UUID.fromString(generatedCorrelationId)).isNotNull();
            assertThat(subscriptionRepository.findById(subscriptionId).orElseThrow().getPauseRequests()).singleElement()
                    .extracting(request -> request.getCorrelationId()).isEqualTo(generatedCorrelationId);
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
            mockMvc.perform(post(URL, subscriptionId.value()).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("SUB-001"));
        }

        @Test
        @DisplayName("Should return 400 VO-014 when pause dates are not ordered")
        void shouldReturn400WhenPauseDatesAreNotOrdered() throws Exception {
            // Arrange
            LocalDate date = LocalDate.now().plusDays(13);
            String body = buildValidCommandJson(date, date.minusDays(1));
            // Act & Assert
            mockMvc.perform(post(URL, subscriptionId.value()).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VO-014"));
        }

        @Test
        @DisplayName("Should return 400 SUB-003 when subscription is not active")
        void shouldReturn400WhenSubscriptionIsAlreadyPaused() throws Exception {
            // Arrange
            Subscription pausedSubscription = subscriptionRepository.findById(subscriptionId).orElseThrow();
            pausedSubscription.pause(new PauseRange(LocalDate.now().plusDays(12), LocalDate.now().plusDays(14)),
                    "Holiday");
            subscriptionRepository.save(pausedSubscription);
            String body = buildValidCommandJson(LocalDate.now().plusDays(6), LocalDate.now().plusDays(8));
            // Act & Assert
            mockMvc.perform(post(URL, subscriptionId.value()).contentType(MediaType.APPLICATION_JSON).content(body))
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
            String body = buildValidCommandJson(LocalDate.now().plusDays(12), LocalDate.now().plusDays(14))
                    .replaceFirst("\"startDate\"\\s*:\\s*\"[^\"]+\"", "\"startDate\":\"invalid\"");
            // Act & Assert
            mockMvc.perform(post(URL, subscriptionId.value()).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_PAYLOAD"));
        }
    }

    private Subscription buildValidCommand() {
        LocalDate start = LocalDate.now().plusDays(10);
        PatientId patientId = PatientId.of("PAC-" + UUID.randomUUID());
        DietPlanId dietPlanId = DietPlanId.of("PLAN-" + UUID.randomUUID());
        return Subscription.create(subscriptionId, patientId, dietPlanId,
                new ServiceContract(dietPlanId, com.mcalvaro.mscatering.domain.dietplan.PlanType.BASE, new ValidityPeriod(start, start.plusDays(14)), ServiceType.FULL,
                        new BigDecimal("250"), "Terms", Instant.now()),
                new DeliveryPreferences(new DeliveryAddress(AddressId.of("DIR-" + UUID.randomUUID()), "Home", "Main", "1", "Lima", null, 0, 0, "1"),
                        new TimeWindow(LocalTime.NOON, LocalTime.of(14, 0)), null),
                1);
    }

    private String buildValidCommandJson(LocalDate start, LocalDate end) throws Exception {
        return objectMapper.writeValueAsString(new PauseSubscriptionCommand(subscriptionId, start, end, "Holiday"));
    }
}
