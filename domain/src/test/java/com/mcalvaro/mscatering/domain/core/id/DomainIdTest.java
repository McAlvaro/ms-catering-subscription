package com.mcalvaro.mscatering.domain.core.id;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcalvaro.mscatering.domain.core.DomainException;
import com.mcalvaro.mscatering.domain.core.DomainId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DomainIdTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("SubscriptionId enforces prefix SUS- and generates correctly")
    void subscriptionIdEnforcesPrefixAndGenerates() {
        SubscriptionId id = SubscriptionId.of("SUS-1001");
        assertThat(id.value()).isEqualTo("SUS-1001");
        assertThat(id.toString()).isEqualTo("SUS-1001");

        SubscriptionId generated = SubscriptionId.generate();
        assertThat(generated.value()).startsWith("SUS-");

        UUID uuid = UUID.randomUUID();
        SubscriptionId fromUuid = SubscriptionId.of(uuid);
        assertThat(fromUuid.value()).isEqualTo("SUS-" + uuid);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "1001", "SUB-1001", "SUS-", "PAC-1001"})
    @DisplayName("SubscriptionId rejects null, blank, wrong prefix, or empty payload")
    void subscriptionIdRejectsInvalidValues(String invalidValue) {
        assertThatThrownBy(() -> SubscriptionId.of(invalidValue))
                .isInstanceOf(DomainException.class);
    }

    @Test
    @DisplayName("DeliveryDayId enforces prefix CD-")
    void deliveryDayIdEnforcesPrefix() {
        DeliveryDayId id = DeliveryDayId.of("CD-70011");
        assertThat(id.value()).isEqualTo("CD-70011");
        assertThat(DeliveryDayId.generate().value()).startsWith("CD-");

        assertThatThrownBy(() -> DeliveryDayId.of("DAY-70011"))
                .isInstanceOf(DomainException.class);
    }

    @Test
    @DisplayName("ConsolidatedCalendarId enforces prefix CAL-")
    void consolidatedCalendarIdEnforcesPrefix() {
        ConsolidatedCalendarId id = ConsolidatedCalendarId.of("CAL-20261030");
        assertThat(id.value()).isEqualTo("CAL-20261030");
        assertThat(ConsolidatedCalendarId.generate().value()).startsWith("CAL-");

        assertThatThrownBy(() -> ConsolidatedCalendarId.of("CC-20261030"))
                .isInstanceOf(DomainException.class);
    }

    @Test
    @DisplayName("PatientId enforces prefix PAC-")
    void patientIdEnforcesPrefix() {
        PatientId id = PatientId.of("PAC-5501");
        assertThat(id.value()).isEqualTo("PAC-5501");
        assertThat(PatientId.generate().value()).startsWith("PAC-");

        assertThatThrownBy(() -> PatientId.of("PAT-5501"))
                .isInstanceOf(DomainException.class);
    }

    @Test
    @DisplayName("AddressId enforces prefix DIR-")
    void addressIdEnforcesPrefix() {
        AddressId id = AddressId.of("DIR-9001");
        assertThat(id.value()).isEqualTo("DIR-9001");
        assertThat(AddressId.generate().value()).startsWith("DIR-");

        assertThatThrownBy(() -> AddressId.of("ADDR-9001"))
                .isInstanceOf(DomainException.class);
    }

    @Test
    @DisplayName("DietPlanId enforces prefix PLAN-")
    void dietPlanIdEnforcesPrefix() {
        DietPlanId id = DietPlanId.of("PLAN-3001");
        assertThat(id.value()).isEqualTo("PLAN-3001");
        assertThat(DietPlanId.generate().value()).startsWith("PLAN-");

        assertThatThrownBy(() -> DietPlanId.of("DIET-3001"))
                .isInstanceOf(DomainException.class);
    }

    @Test
    @DisplayName("RecipeId enforces prefix REC-")
    void recipeIdEnforcesPrefix() {
        RecipeId id = RecipeId.of("REC-4001");
        assertThat(id.value()).isEqualTo("REC-4001");
        assertThat(RecipeId.generate().value()).startsWith("REC-");

        assertThatThrownBy(() -> RecipeId.of("RCP-4001"))
                .isInstanceOf(DomainException.class);
    }

    @Test
    @DisplayName("DeliveryId enforces prefix ENT-")
    void deliveryIdEnforcesPrefix() {
        DeliveryId id = DeliveryId.of("ENT-8001");
        assertThat(id.value()).isEqualTo("ENT-8001");
        assertThat(DeliveryId.generate().value()).startsWith("ENT-");

        assertThatThrownBy(() -> DeliveryId.of("DEL-8001"))
                .isInstanceOf(DomainException.class);
    }

    @Test
    @DisplayName("PackageId enforces prefix PKG-")
    void packageIdEnforcesPrefix() {
        PackageId id = PackageId.of("PKG-20261030-001");
        assertThat(id.value()).isEqualTo("PKG-20261030-001");
        assertThat(PackageId.generate().value()).startsWith("PKG-");

        assertThatThrownBy(() -> PackageId.of("PACK-001"))
                .isInstanceOf(DomainException.class);
    }

    @Test
    @DisplayName("EvaluationId enforces prefix EVAL-")
    void evaluationIdEnforcesPrefix() {
        EvaluationId id = EvaluationId.of("EVAL-101");
        assertThat(id.value()).isEqualTo("EVAL-101");
        assertThat(EvaluationId.generate().value()).startsWith("EVAL-");

        assertThatThrownBy(() -> EvaluationId.of("EV-101"))
                .isInstanceOf(DomainException.class);
    }

    @Test
    @DisplayName("Auxiliary IDs enforce respective prefixes")
    void auxiliaryIdsEnforcePrefixes() {
        assertThat(DeliveryCalendarId.of("DCAL-1").value()).isEqualTo("DCAL-1");
        assertThat(DeliveryCalendarId.generate().value()).startsWith("DCAL-");

        assertThat(PauseRequestId.of("PAU-1").value()).isEqualTo("PAU-1");
        assertThat(PauseRequestId.generate().value()).startsWith("PAU-");

        assertThat(ConsolidatedLineId.of("LINE-1").value()).isEqualTo("LINE-1");
        assertThat(ConsolidatedLineId.generate().value()).startsWith("LINE-");

        assertThat(ConsolidatedLineRecipeId.of("CLR-1").value()).isEqualTo("CLR-1");
        assertThat(ConsolidatedLineRecipeId.generate().value()).startsWith("CLR-");
    }

    @Test
    @DisplayName("JSON serialization and deserialization preserves value transparency")
    void jsonSerializationRoundtrip() throws Exception {
        SubscriptionId original = SubscriptionId.of("SUS-test-123");
        String json = objectMapper.writeValueAsString(original);
        assertThat(json).isEqualTo("\"SUS-test-123\"");

        SubscriptionId deserialized = objectMapper.readValue(json, SubscriptionId.class);
        assertThat(deserialized).isEqualTo(original);
    }

    @Test
    @DisplayName("Value equality and hashCode consistency")
    void equalityAndHashCode() {
        SubscriptionId id1 = SubscriptionId.of("SUS-ABC");
        SubscriptionId id2 = SubscriptionId.of("SUS-ABC");
        SubscriptionId id3 = SubscriptionId.of("SUS-XYZ");

        assertThat(id1).isEqualTo(id2);
        assertThat(id1.hashCode()).isEqualTo(id2.hashCode());
        assertThat(id1).isNotEqualTo(id3);
        assertThat(id1).isInstanceOf(DomainId.class);
    }
}
