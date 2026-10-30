package com.mcalvaro.mscatering.infrastructure.integration.outbound.kafka;

import com.mcalvaro.mscatering.integration.contracts.ConsolidatedCalendarClosedIntegrationEvent;
import com.mcalvaro.mscatering.integration.contracts.IntegrationEvent;
import com.mcalvaro.mscatering.integration.contracts.SubscriptionCancelledIntegrationEvent;
import com.mcalvaro.mscatering.integration.contracts.SubscriptionCreatedIntegrationEvent;
import com.mcalvaro.mscatering.integration.contracts.SubscriptionPausedIntegrationEvent;
import com.mcalvaro.mscatering.integration.contracts.SubscriptionReactivatedIntegrationEvent;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

final class KafkaIntegrationEventFixtures {
    static final UUID EVENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    static final UUID SUBSCRIPTION_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    static final UUID PATIENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    static final UUID PLAN_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");
    static final UUID CALENDAR_ID = UUID.fromString("00000000-0000-0000-0000-000000000005");
    static final Instant OCCURRED_ON = Instant.parse("2026-10-29T12:00:00Z");

    private KafkaIntegrationEventFixtures() {
    }

    static List<IntegrationEvent> allEvents() {
        return List.of(created(), paused(), reactivated(), cancelled(), calendarClosed());
    }

    static SubscriptionCreatedIntegrationEvent created() {
        return new SubscriptionCreatedIntegrationEvent(EVENT_ID, OCCURRED_ON, "correlation",
                new SubscriptionCreatedIntegrationEvent.Data(SUBSCRIPTION_ID, PATIENT_ID, PLAN_ID, "PERSONALIZADO",
                        30, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 30), 2,
                        new BigDecimal("250.50"), null));
    }

    static SubscriptionPausedIntegrationEvent paused() {
        return new SubscriptionPausedIntegrationEvent(EVENT_ID, OCCURRED_ON, "correlation",
                new SubscriptionPausedIntegrationEvent.Data(SUBSCRIPTION_ID, PATIENT_ID, LocalDate.of(2026, 10, 5),
                        LocalDate.of(2026, 10, 8), "Viaje", LocalDate.of(2026, 11, 2), LocalDate.of(2026, 10, 1)));
    }

    static SubscriptionReactivatedIntegrationEvent reactivated() {
        return new SubscriptionReactivatedIntegrationEvent(EVENT_ID, OCCURRED_ON, "correlation",
                new SubscriptionReactivatedIntegrationEvent.Data(SUBSCRIPTION_ID, PATIENT_ID, LocalDate.of(2026, 10, 8),
                        LocalDate.of(2026, 11, 2)));
    }

    static SubscriptionCancelledIntegrationEvent cancelled() {
        return new SubscriptionCancelledIntegrationEvent(EVENT_ID, OCCURRED_ON, "correlation",
                new SubscriptionCancelledIntegrationEvent.Data(SUBSCRIPTION_ID, PATIENT_ID, PLAN_ID, "MANUAL", "Solicitud",
                        LocalDate.of(2026, 10, 8), 1));
    }

    static ConsolidatedCalendarClosedIntegrationEvent calendarClosed() {
        return new ConsolidatedCalendarClosedIntegrationEvent(EVENT_ID, OCCURRED_ON, "correlation",
                new ConsolidatedCalendarClosedIntegrationEvent.Data(CALENDAR_ID, LocalDate.of(2026, 10, 29), OCCURRED_ON, 1,
                        List.of(new ConsolidatedCalendarClosedIntegrationEvent.Delivery(
                                UUID.fromString("00000000-0000-0000-0000-000000000006"), SUBSCRIPTION_ID, PATIENT_ID,
                                "Ana", PLAN_ID, 1,
                                new ConsolidatedCalendarClosedIntegrationEvent.Address(
                                        UUID.fromString("00000000-0000-0000-0000-000000000007"), "Casa", "Calle 1",
                                        "Referencia", "Santa Cruz", -17.78, -63.18),
                                List.of(new ConsolidatedCalendarClosedIntegrationEvent.Recipe(
                                        UUID.fromString("00000000-0000-0000-0000-000000000008"), "Sopa", "08:30",
                                        new BigDecimal("250")))))));
    }
}
