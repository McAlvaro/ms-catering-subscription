package com.mcalvaro.mscatering.domain.consolidatedcalendar;

import com.mcalvaro.mscatering.domain.consolidatedcalendar.entity.ConsolidatedLine;
import com.mcalvaro.mscatering.domain.consolidatedcalendar.entity.ConsolidatedRecipeSnapshot;
import com.mcalvaro.mscatering.domain.consolidatedcalendar.event.ConsolidatedCalendarClosed;
import com.mcalvaro.mscatering.domain.subscription.enums.ServiceType;
import com.mcalvaro.mscatering.domain.subscription.vo.DeliveryAddress;
import com.mcalvaro.mscatering.domain.subscription.vo.TimeWindow;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ConsolidatedCalendarCloseSnapshotTest {

    @Test
    void closesWithOneSharedInstantCorrelationAndDeeplyImmutableDeliverySnapshot() {
        UUID calendarId = UUID.randomUUID();
        UUID addressId = UUID.randomUUID();
        ConsolidatedCalendar calendar = ConsolidatedCalendar.create(calendarId, LocalDate.of(2026, 10, 28));
        calendar.addLine(new ConsolidatedLine(UUID.randomUUID(), calendarId, UUID.randomUUID(), UUID.randomUUID(),
                "Ada Lovelace", UUID.randomUUID(), 2, ServiceType.LUNCH,
                new DeliveryAddress(addressId, "Office", "Main", null, "Madrid", "Reception", 40.4, -3.7, null),
                new TimeWindow(LocalTime.NOON, LocalTime.of(14, 0)), "", List.of(
                        new ConsolidatedRecipeSnapshot(UUID.randomUUID(), "Soup", LocalTime.of(12, 0), 250))));

        Instant closedAt = Instant.parse("2026-10-28T18:00:00Z");
        calendar.close("scheduler", "corr-42", closedAt);

        ConsolidatedCalendarClosed event = (ConsolidatedCalendarClosed) calendar.getDomainEvents().get(1);
        assertThat(calendar.getClosedAt()).isEqualTo(closedAt);
        assertThat(event.occurredAt()).isEqualTo(closedAt);
        assertThat(event.correlationId()).isEqualTo("corr-42");
        assertThat(event.deliveries()).singleElement().satisfies(delivery -> {
            assertThat(delivery.patientName()).isEqualTo("Ada Lovelace");
            assertThat(delivery.address().addressId()).isEqualTo(addressId);
            assertThat(delivery.address().alias()).isEqualTo("Office");
            assertThat(delivery.recipes()).extracting(ConsolidatedRecipeSnapshot::portionGrams).containsExactly(250);
        });
        assertThat(event.deliveries()).isUnmodifiable();
        assertThat(event.deliveries().get(0).recipes()).isUnmodifiable();
    }
}
