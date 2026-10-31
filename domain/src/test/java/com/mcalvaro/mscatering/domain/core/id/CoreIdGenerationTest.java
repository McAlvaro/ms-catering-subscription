package com.mcalvaro.mscatering.domain.core.id;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CoreIdGenerationTest {

    @Test
    @DisplayName("generate() produces 8-char hex IDs with expected prefix")
    void shouldGenerateCompactIds() {
        SubscriptionId subId = SubscriptionId.generate();
        assertThat(subId.value()).matches("^SUS-[0-9A-F]{8}$");

        DeliveryDayId dayId = DeliveryDayId.generate();
        assertThat(dayId.value()).matches("^CD-[0-9A-F]{8}$");

        DeliveryCalendarId calId = DeliveryCalendarId.generate();
        assertThat(calId.value()).matches("^DCAL-[0-9A-F]{8}$");

        ConsolidatedCalendarId consCalId = ConsolidatedCalendarId.generate();
        assertThat(consCalId.value()).matches("^CAL-[0-9A-F]{8}$");
    }

    @Test
    @DisplayName("ConsolidatedCalendarId.ofDate() formats date as BASIC_ISO_DATE")
    void shouldCreateConsolidatedCalendarIdFromDate() {
        LocalDate date = LocalDate.of(2026, 4, 15);
        ConsolidatedCalendarId id = ConsolidatedCalendarId.ofDate(date);

        assertThat(id.value()).isEqualTo("CAL-20260415");
    }

    @Test
    @DisplayName("ConsolidatedCalendarId.ofDate(null) throws NullPointerException")
    void shouldThrowWhenDateIsNull() {
        assertThatThrownBy(() -> ConsolidatedCalendarId.ofDate(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("date must not be null");
    }

    @Test
    @DisplayName("Backward compatibility is preserved for String and UUID factories")
    void shouldPreserveBackwardCompatibility() {
        UUID uuid = UUID.randomUUID();
        String legacyId = "SUS-" + uuid;

        SubscriptionId fromString = SubscriptionId.of(legacyId);
        SubscriptionId fromUuid = SubscriptionId.of(uuid);

        assertThat(fromString.value()).isEqualTo(legacyId);
        assertThat(fromUuid.value()).isEqualTo(legacyId);
    }
}
