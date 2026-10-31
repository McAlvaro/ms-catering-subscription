package com.mcalvaro.mscatering.infrastructure.api.subscription;

import com.mcalvaro.mscatering.application.subscription.UpdateDeliveryPreferences.UpdateDeliveryPreferencesCommand;
import com.mcalvaro.mscatering.domain.core.id.AddressId;
import com.mcalvaro.mscatering.domain.core.id.DietPlanId;
import com.mcalvaro.mscatering.domain.core.id.PatientId;
import com.mcalvaro.mscatering.domain.core.id.SubscriptionId;
import com.mcalvaro.mscatering.domain.subscription.*;
import com.mcalvaro.mscatering.domain.subscription.enums.ServiceType;
import com.mcalvaro.mscatering.domain.subscription.vo.*;
import com.mcalvaro.mscatering.infrastructure.BaseIntegrationTest;
import com.mcalvaro.mscatering.infrastructure.support.PatientAddressRegistrationFixture;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class UpdateDeliveryPreferencesApiIT extends BaseIntegrationTest {
    private static final String URL = "/api/subscriptions/{id}/preferences";
    @Autowired
    private ISubscriptionRepository subscriptionRepository;
    private SubscriptionId subscriptionId;
    private PatientId patientId;
    private String deliveryAddressId;
    private String foreignAddressId;

    @Autowired
    private PatientAddressRegistrationFixture addressFixture;

    @BeforeEach
    void setUp() {
        subscriptionId = SubscriptionId.of("SUS-" + UUID.randomUUID());
        patientId = PatientId.of("PAC-" + UUID.randomUUID());
        deliveryAddressId = addressFixture.ingestAddress(patientId.value());
        foreignAddressId = addressFixture.ingestAddress("PAC-" + UUID.randomUUID());
        subscriptionRepository.save(buildValidCommand());
    }

    @Nested
    @DisplayName("Happy Path")
    class HappyPath {
        @Test
        @DisplayName("Should return 200 OK when delivery preferences are valid")
        void shouldReturn200WhenPreferencesAreValid() throws Exception {// Arrange
            String body = buildValidCommandJson("New Street", LocalTime.of(10, 0), LocalTime.of(12, 0));
            // Act & Assert
            mockMvc.perform(put(URL, subscriptionId.value()).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Domain Rule Violations")
    class DomainRuleViolations {
        @Test
        @DisplayName("Should return 400 ADR-001 when the address is unavailable")
        void shouldReturn400WhenAddressIsMissing() throws Exception {// Arrange
            String body = buildValidCommandJson(" ", LocalTime.of(10, 0), LocalTime.of(12, 0))
                    .replace(deliveryAddressId, "DIR-00000000-0000-0000-0000-000000000002");
            // Act & Assert
            mockMvc.perform(put(URL, subscriptionId.value()).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("ADR-001"));
        }

        @Test
        @DisplayName("Should return 400 ADR-001 when the delivery address belongs to another patient")
        void shouldReturn400WhenAddressIsForeign() throws Exception {// Arrange
            String body = buildValidCommandJson("New Street", LocalTime.of(10, 0), LocalTime.of(12, 0))
                    .replace(deliveryAddressId, foreignAddressId);
            // Act & Assert
            mockMvc.perform(put(URL, subscriptionId.value()).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("ADR-001"));
        }

        @Test
        @DisplayName("Should return 400 VO-002 when time window is invalid")
        void shouldReturn400WhenTimeWindowIsInvalid() throws Exception {// Arrange
            String body = buildValidCommandJson("New Street", LocalTime.of(12, 0), LocalTime.of(10, 0));
            // Act & Assert
            mockMvc.perform(put(URL, subscriptionId.value()).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VO-002"));
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
            mockMvc.perform(put(URL, subscriptionId.value()).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_PAYLOAD"));
        }
    }

    private Subscription buildValidCommand() {
        LocalDate start = LocalDate.now().plusDays(10);
        DietPlanId dietPlanId = DietPlanId.of("PLAN-" + UUID.randomUUID());
        return Subscription.create(subscriptionId, patientId, dietPlanId,
                new ServiceContract(dietPlanId, com.mcalvaro.mscatering.domain.dietplan.PlanType.BASE, new ValidityPeriod(start, start.plusDays(14)), ServiceType.FULL,
                        new BigDecimal("250"), "Terms", Instant.now()),
                new DeliveryPreferences(new DeliveryAddress(AddressId.of(deliveryAddressId), "Home", "Main", "1", "Lima", null, 0, 0, "1"),
                        new TimeWindow(LocalTime.NOON, LocalTime.of(14, 0)), null),
                1);
    }

    private String buildValidCommandJson(String street, LocalTime start, LocalTime end) throws Exception {
        return objectMapper.writeValueAsString(new UpdateDeliveryPreferencesCommand(subscriptionId.value(), deliveryAddressId,
                start, end, null));
    }
}
