package com.mcalvaro.mscatering.infrastructure.api.subscription;

import com.mcalvaro.mscatering.domain.core.id.AddressId;
import com.mcalvaro.mscatering.domain.core.id.DietPlanId;
import com.mcalvaro.mscatering.domain.core.id.EvaluationId;
import com.mcalvaro.mscatering.domain.core.id.PatientId;
import com.mcalvaro.mscatering.domain.core.id.SubscriptionId;
import com.mcalvaro.mscatering.domain.subscription.*;
import com.mcalvaro.mscatering.domain.subscription.entity.BiweeklyEvaluation;
import com.mcalvaro.mscatering.domain.subscription.enums.ServiceType;
import com.mcalvaro.mscatering.domain.subscription.vo.*;
import com.mcalvaro.mscatering.infrastructure.BaseIntegrationTest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.UUID;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class MarkEvaluationCompletedApiIT extends BaseIntegrationTest {
    private static final String URL = "/api/subscriptions/{id}/evaluations/{evalId}/complete";
    @Autowired
    private ISubscriptionRepository subscriptionRepository;
    private SubscriptionId subscriptionId;
    private EvaluationId evaluationId;

    @BeforeEach
    void setUp() {
        subscriptionId = SubscriptionId.of("SUS-" + UUID.randomUUID());
        evaluationId = EvaluationId.of("EVAL-" + UUID.randomUUID());
        Subscription s = buildValidCommand();
        s.scheduleEvaluations(
                List.of(new BiweeklyEvaluation(evaluationId, s.getPatientId(), 1, LocalDate.now().plusDays(14))));
        subscriptionRepository.save(s);
    }

    @Nested
    @DisplayName("Happy Path")
    class HappyPath {
        @Test
        @DisplayName("Should return 200 OK when evaluation exists")
        void shouldReturn200WhenEvaluationExists() throws Exception {// Arrange
            // Act & Assert
            mockMvc.perform(post(URL, subscriptionId.value(), evaluationId.value()).param("completedAt", LocalDate.now().toString()))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Domain Rule Violations")
    class DomainRuleViolations {
        @Test
        @DisplayName("Should return 404 NOT_FOUND when evaluation does not exist")
        void shouldReturn404WhenEvaluationDoesNotExist() throws Exception {// Arrange
            // Act & Assert
            mockMvc.perform(
                    post(URL, subscriptionId.value(), "EVAL-" + UUID.randomUUID()).param("completedAt", LocalDate.now().toString()))
                    .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("NOT_FOUND"));
        }
    }

    @Nested
    @DisplayName("Malformed Payload")
    class MalformedPayload {
        @Test
        @DisplayName("Should return 500 INTERNAL_SERVER_ERROR when completedAt is malformed")
        void shouldReturn500WhenCompletedAtIsMalformed() throws Exception {// Arrange
            // Act & Assert
            mockMvc.perform(post(URL, subscriptionId.value(), evaluationId.value()).param("completedAt", "invalid"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"));
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
}
