package com.mcalvaro.mscatering.infrastructure.api.subscription;

import com.mcalvaro.mscatering.application.subscription.ModifyDeliveryDay.ModifyDeliveryDayCommand;
import com.mcalvaro.mscatering.domain.core.id.AddressId;
import com.mcalvaro.mscatering.domain.core.id.DeliveryDayId;
import com.mcalvaro.mscatering.domain.core.id.DietPlanId;
import com.mcalvaro.mscatering.domain.core.id.PatientId;
import com.mcalvaro.mscatering.domain.core.id.SubscriptionId;
import com.mcalvaro.mscatering.domain.subscription.*;
import com.mcalvaro.mscatering.domain.subscription.enums.ServiceType;
import com.mcalvaro.mscatering.domain.subscription.vo.*;
import com.mcalvaro.mscatering.infrastructure.BaseIntegrationTest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ModifyDeliveryDayApiIT extends BaseIntegrationTest {
    private static final String URL = "/api/subscriptions/{id}/delivery-days/{dayId}";
    @Autowired
    private ISubscriptionRepository subscriptionRepository;
    private SubscriptionId subscriptionId;
    private DeliveryDayId deliveryDayId;

    @BeforeEach
    void setUp() {
        subscriptionId = SubscriptionId.of("SUS-" + UUID.randomUUID());
        Subscription s = buildValidCommand();
        deliveryDayId = s.getDeliveryCalendar().getDeliveryDays().get(0).getId();
        subscriptionRepository.save(s);
    }

    @Nested
    @DisplayName("Happy Path")
    class HappyPath {
        @Test
        @DisplayName("Should return 200 OK when delivery day is more than 48 hours away")
        void shouldReturn200WhenDayCanBeModified() throws Exception {// Arrange
            String body = buildValidCommandJson("New Street", LocalTime.of(10, 0), LocalTime.of(12, 0));
            // Act & Assert
            mockMvc.perform(
                    put(URL, subscriptionId.value(), deliveryDayId.value()).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Domain Rule Violations")
    class DomainRuleViolations {
        @Test
        @DisplayName("Should return 400 SUB-009 when delivery day does not exist")
        void shouldReturn400WhenDayDoesNotExist() throws Exception {// Arrange
            String body = buildValidCommandJson("New Street", LocalTime.of(10, 0), LocalTime.of(12, 0));
            // Act & Assert
            mockMvc.perform(
                    put(URL, subscriptionId.value(), "CD-" + UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("SUB-009"));
        }

        @Test
        @DisplayName("Should return 400 VO-003 when street is blank")
        void shouldReturn400WhenStreetIsBlank() throws Exception {// Arrange
            String body = buildValidCommandJson(" ", LocalTime.of(10, 0), LocalTime.of(12, 0));
            // Act & Assert
            mockMvc.perform(
                    put(URL, subscriptionId.value(), deliveryDayId.value()).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VO-003"));
        }

        @Test
        @DisplayName("Should return 400 VO-002 when time window is invalid")
        void shouldReturn400WhenTimeWindowIsInvalid() throws Exception {// Arrange
            String body = buildValidCommandJson("New Street", LocalTime.of(12, 0), LocalTime.of(10, 0));
            // Act & Assert
            mockMvc.perform(
                    put(URL, subscriptionId.value(), deliveryDayId.value()).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VO-002"));
        }

        @Test
        @DisplayName("Should return 400 SUB-002 when delivery is within 48 hours")
        void shouldReturn400WhenDayIsWithin48Hours() throws Exception {// Arrange
            SubscriptionId nearId = SubscriptionId.of("SUS-" + UUID.randomUUID());
            Subscription near = buildSubscription(nearId, LocalDate.now().plusDays(1));
            DeliveryDayId nearDayId = near.getDeliveryCalendar().getDeliveryDays().get(0).getId();
            subscriptionRepository.save(near);
            String body = objectMapper.writeValueAsString(new ModifyDeliveryDayCommand(nearId, nearDayId, "New Street",
                    "2", "Lima", null, 0, 0, "2", LocalTime.of(10, 0), LocalTime.of(12, 0), null));
            // Act & Assert
            mockMvc.perform(put(URL, nearId.value(), nearDayId.value()).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("SUB-002"));
        }

        @Test
        @DisplayName("Should return 400 SUB-011 when delivery day is consolidated")
        void shouldReturn400WhenDayIsBlocked() throws Exception {// Arrange
            Subscription blockedSubscription = subscriptionRepository.findById(subscriptionId).orElseThrow();
            blockedSubscription.getDeliveryCalendar().findDayById(deliveryDayId).orElseThrow().markAsConsolidated();
            subscriptionRepository.save(blockedSubscription);
            String body = buildValidCommandJson("New Street", LocalTime.of(10, 0), LocalTime.of(12, 0));
            // Act & Assert
            mockMvc.perform(
                    put(URL, subscriptionId.value(), deliveryDayId.value()).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("SUB-011"));
        }
    }

    @Nested
    @DisplayName("Malformed Payload")
    class MalformedPayload {
        @Test
        @DisplayName("Should return 400 INVALID_PAYLOAD when startTime is malformed")
        void shouldReturn400WhenStartTimeIsMalformed() throws Exception {// Arrange
            String body = buildValidCommandJson("New Street", LocalTime.of(10, 0), LocalTime.of(12, 0))
                    .replaceFirst("\"startTime\"\\s*:\\s*\"[^\"]+\"", "\"startTime\":\"invalid\"");
            // Act & Assert
            mockMvc.perform(
                    put(URL, subscriptionId.value(), deliveryDayId.value()).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_PAYLOAD"));
        }
    }

    private Subscription buildValidCommand() {
        return buildSubscription(subscriptionId, LocalDate.now().plusDays(10));
    }

    private Subscription buildSubscription(SubscriptionId id, LocalDate start) {
        PatientId patientId = PatientId.of("PAC-" + UUID.randomUUID());
        DietPlanId dietPlanId = DietPlanId.of("PLAN-" + UUID.randomUUID());
        return Subscription.create(id, patientId, dietPlanId,
                new ServiceContract(dietPlanId, com.mcalvaro.mscatering.domain.dietplan.PlanType.BASE, new ValidityPeriod(start, start.plusDays(14)), ServiceType.FULL,
                        new BigDecimal("250"), "Terms", Instant.now()),
                new DeliveryPreferences(new DeliveryAddress(AddressId.of("DIR-" + UUID.randomUUID()), "Home", "Main", "1", "Lima", null, 0, 0, "1"),
                        new TimeWindow(LocalTime.NOON, LocalTime.of(14, 0)), null),
                1);
    }

    private String buildValidCommandJson(String street, LocalTime start, LocalTime end) throws Exception {
        return objectMapper.writeValueAsString(new ModifyDeliveryDayCommand(subscriptionId, deliveryDayId, street, "2",
                "Lima", null, 0, 0, "2", start, end, null));
    }
}
