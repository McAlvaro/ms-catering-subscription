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

class ConfirmDeliveryApiIT extends BaseIntegrationTest {
    private static final String URL = "/api/subscriptions/{id}/delivery-days/{dayId}/confirm";
    @Autowired
    private ISubscriptionRepository subscriptionRepository;
    private UUID subscriptionId;
    private UUID deliveryDayId;

    @BeforeEach
    void setUp() {
        subscriptionId = UUID.randomUUID();
        Subscription s = buildValidCommand();
        deliveryDayId = s.getDeliveryCalendar().getDeliveryDays().get(0).getId();
        subscriptionRepository.save(s);
    }

    @Nested
    @DisplayName("Happy Path")
    class HappyPath {
        @Test
        @DisplayName("Should return 200 OK when delivery is confirmed")
        void shouldReturn200WhenDayExists() throws Exception {// Arrange
            // Act & Assert
            mockMvc.perform(post(URL, subscriptionId, deliveryDayId)).andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Domain Rule Violations")
    class DomainRuleViolations {
        @Test
        @DisplayName("Should return 400 SUB-009 when delivery day does not exist")
        void shouldReturn400WhenDayDoesNotExist() throws Exception {// Arrange
            // Act & Assert
            mockMvc.perform(post(URL, subscriptionId, UUID.randomUUID())).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("SUB-009"));
        }
    }

    @Nested
    @DisplayName("Malformed Payload")
    class MalformedPayload {
        @Test
        @DisplayName("Should return 500 INTERNAL_SERVER_ERROR when delivery day ID is malformed")
        void shouldReturn500WhenDayIdIsMalformed() throws Exception {// Arrange
            // Act & Assert
            mockMvc.perform(post(URL, subscriptionId, "invalid")).andExpect(status().isInternalServerError())
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
