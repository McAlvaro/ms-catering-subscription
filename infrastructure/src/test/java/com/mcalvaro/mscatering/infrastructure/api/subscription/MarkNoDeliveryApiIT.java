package com.mcalvaro.mscatering.infrastructure.api.subscription;

import com.mcalvaro.mscatering.domain.core.id.AddressId;
import com.mcalvaro.mscatering.domain.core.id.DeliveryDayId;
import com.mcalvaro.mscatering.domain.core.id.DietPlanId;
import com.mcalvaro.mscatering.domain.core.id.PatientId;
import com.mcalvaro.mscatering.domain.core.id.SubscriptionId;
import com.mcalvaro.mscatering.domain.subscription.*;
import com.mcalvaro.mscatering.domain.subscription.entity.DeliveryDay;
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

class MarkNoDeliveryApiIT extends BaseIntegrationTest {
    private static final String URL = "/api/subscriptions/{id}/delivery-days/{dayId}/no-delivery";
    @Autowired
    private ISubscriptionRepository subscriptionRepository;
    private SubscriptionId subscriptionId;
    private DeliveryDayId deliveryDayId;

    @BeforeEach
    void setUp() {
        subscriptionId = SubscriptionId.of("SUS-" + UUID.randomUUID());
        Subscription subscription = buildValidCommand();
        deliveryDayId = subscription.getDeliveryCalendar().getDeliveryDays().get(0).getId();
        subscriptionRepository.save(subscription);
    }

    @Nested
    @DisplayName("Happy Path")
    class HappyPath {
        @Test
        @DisplayName("Should return 200 OK when a delivery day is marked as no delivery")
        void shouldReturn200WhenDayIsActive() throws Exception {
            // Arrange
            // Act & Assert
            mockMvc.perform(post(URL, subscriptionId.value(), deliveryDayId.value())).andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Domain Rule Violations")
    class DomainRuleViolations {
        @Test
        @DisplayName("Should return 400 SUB-009 when delivery day does not exist")
        void shouldReturn400WhenDeliveryDayDoesNotExist() throws Exception {
            // Arrange
            // Act & Assert
            mockMvc.perform(post(URL, subscriptionId.value(), "CD-" + UUID.randomUUID())).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("SUB-009"));
        }

        @Test
        @DisplayName("Should return 400 SUB-012 when the calendar would have no active days")
        void shouldReturn400WhenNoActiveDaysWouldRemain() throws Exception {
            // Arrange
            Subscription subscription = subscriptionRepository.findById(subscriptionId).orElseThrow();
            for (DeliveryDay day : subscription.getDeliveryCalendar().getDeliveryDays())
                if (!day.getId().equals(deliveryDayId))
                    day.markAsNoDelivery();
            subscriptionRepository.save(subscription);
            // Act & Assert
            mockMvc.perform(post(URL, subscriptionId.value(), deliveryDayId.value())).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("SUB-012"));
        }
    }

    @Nested
    @DisplayName("Malformed Payload")
    class MalformedPayload {
        @Test
        @DisplayName("Should return 500 INTERNAL_SERVER_ERROR when delivery day ID is not a UUID")
        void shouldReturn500WhenDeliveryDayIdIsMalformed() throws Exception {
            // Arrange
            // Act & Assert
            mockMvc.perform(post(URL, subscriptionId.value(), "invalid")).andExpect(status().isInternalServerError())
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
