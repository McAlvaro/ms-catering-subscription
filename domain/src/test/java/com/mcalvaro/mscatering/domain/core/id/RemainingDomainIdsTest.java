package com.mcalvaro.mscatering.domain.core.id;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RemainingDomainIdsTest {

    @Test
    @DisplayName("All remaining domain IDs generate compact 8-char hex values")
    void shouldGenerateCompactIdsForRemainingDomainEntities() {
        assertThat(ConsolidatedLineId.generate().value()).matches("^LINE-[0-9A-F]{8}$");
        assertThat(ConsolidatedLineRecipeId.generate().value()).matches("^CLR-[0-9A-F]{8}$");
        assertThat(EvaluationId.generate().value()).matches("^EVAL-[0-9A-F]{8}$");
        assertThat(PauseRequestId.generate().value()).matches("^PAU-[0-9A-F]{8}$");
        assertThat(PatientId.generate().value()).matches("^PAC-[0-9A-F]{8}$");
        assertThat(DietPlanId.generate().value()).matches("^PLAN-[0-9A-F]{8}$");
        assertThat(AddressId.generate().value()).matches("^DIR-[0-9A-F]{8}$");
        assertThat(DeliveryId.generate().value()).matches("^ENT-[0-9A-F]{8}$");
        assertThat(PackageId.generate().value()).matches("^PKG-[0-9A-F]{8}$");
        assertThat(RecipeId.generate().value()).matches("^REC-[0-9A-F]{8}$");
    }

    @Test
    @DisplayName("Backward compatibility is preserved for of(UUID)")
    void shouldSupportLegacyUuidInAllRemainingIds() {
        UUID uuid = UUID.randomUUID();

        assertThat(ConsolidatedLineId.of(uuid).value()).isEqualTo("LINE-" + uuid);
        assertThat(ConsolidatedLineRecipeId.of(uuid).value()).isEqualTo("CLR-" + uuid);
        assertThat(EvaluationId.of(uuid).value()).isEqualTo("EVAL-" + uuid);
        assertThat(PauseRequestId.of(uuid).value()).isEqualTo("PAU-" + uuid);
        assertThat(PatientId.of(uuid).value()).isEqualTo("PAC-" + uuid);
        assertThat(DietPlanId.of(uuid).value()).isEqualTo("PLAN-" + uuid);
        assertThat(AddressId.of(uuid).value()).isEqualTo("DIR-" + uuid);
        assertThat(DeliveryId.of(uuid).value()).isEqualTo("ENT-" + uuid);
        assertThat(PackageId.of(uuid).value()).isEqualTo("PKG-" + uuid);
        assertThat(RecipeId.of(uuid).value()).isEqualTo("REC-" + uuid);
    }
}
