package com.mcalvaro.mscatering.integration.contracts;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IntegrationEventInvariantTest {
    private static final UUID EVENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final String ID = "SUS-00000000-0000-0000-0000-000000000001";

    @Test
    void rejectsBlankRequiredFieldsAndInvalidContractRanges() {
        assertThatThrownBy(() -> new SubscriptionCreatedIntegrationEvent(EVENT_ID, Instant.now(), " ", createdData()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SubscriptionCreatedIntegrationEvent.Data(ID, ID, ID, "BASE", 0,
                LocalDate.now(), LocalDate.now(), 0, BigDecimal.ZERO, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ConsolidatedCalendarClosedIntegrationEvent.Address(ID, "Casa", "Calle", "Ref", "Ciudad", 91, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ConsolidatedCalendarClosedIntegrationEvent.Recipe(ID, "Sopa", "8:05", BigDecimal.ONE))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ConsolidatedCalendarClosedIntegrationEvent.Recipe(ID, "Sopa", "08:05", BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void makesDeliveriesAndRecipesDefensiveAndRequiresMatchingNonEmptyTotal() {
        List<ConsolidatedCalendarClosedIntegrationEvent.Recipe> recipes = new ArrayList<>();
        recipes.add(new ConsolidatedCalendarClosedIntegrationEvent.Recipe(ID, "Sopa", "08:05", BigDecimal.ONE));
        ConsolidatedCalendarClosedIntegrationEvent.Delivery delivery = new ConsolidatedCalendarClosedIntegrationEvent.Delivery(
                ID, ID, ID, "Ana", ID, 1,
                new ConsolidatedCalendarClosedIntegrationEvent.Address(ID, "Casa", "Calle", "Ref", "Ciudad", 0, 0), recipes);
        recipes.clear();
        assertThat(delivery.recipes()).hasSize(1);
        assertThatThrownBy(() -> delivery.recipes().add(delivery.recipes().get(0))).isInstanceOf(UnsupportedOperationException.class);

        assertThatThrownBy(() -> new ConsolidatedCalendarClosedIntegrationEvent.Data(ID, LocalDate.now(), Instant.now(), 2, List.of(delivery)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ConsolidatedCalendarClosedIntegrationEvent.Data(ID, LocalDate.now(), Instant.now(), 0, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static SubscriptionCreatedIntegrationEvent.Data createdData() {
        return new SubscriptionCreatedIntegrationEvent.Data(ID, ID, ID, "BASE", 1, LocalDate.now(), LocalDate.now(), 0, BigDecimal.ZERO, null);
    }
}
