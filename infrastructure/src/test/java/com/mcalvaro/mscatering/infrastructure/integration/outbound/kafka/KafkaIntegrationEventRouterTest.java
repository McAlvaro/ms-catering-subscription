package com.mcalvaro.mscatering.infrastructure.integration.outbound.kafka;

import com.mcalvaro.mscatering.integration.contracts.IntegrationEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KafkaIntegrationEventRouterTest {
    private static final String TOPIC = "bc3.catering.eventos";
    private final KafkaIntegrationEventRouter router = new KafkaIntegrationEventRouter(TOPIC);

    @Test
    void routesEveryApprovedExactTypeToTheSingleTopicUsingItsAggregateKey() {
        for (IntegrationEvent event : KafkaIntegrationEventFixtures.allEvents()) {
            KafkaIntegrationEventRouter.Route route = router.route(event);

            assertThat(route.topic()).isEqualTo(TOPIC);
            assertThat(route.key()).isEqualTo(event instanceof com.mcalvaro.mscatering.integration.contracts.ConsolidatedCalendarClosedIntegrationEvent
                    ? KafkaIntegrationEventFixtures.CALENDAR_ID.toString()
                    : KafkaIntegrationEventFixtures.SUBSCRIPTION_ID.toString());
        }
    }

    @Test
    void failsClosedForNullUnknownAndUnexpectedLogicalDestinations() {
        assertThatIllegalArgumentException().isThrownBy(() -> router.route(null));
        assertThatIllegalArgumentException().isThrownBy(() -> router.route(new IntegrationEvent() {
            @Override public UUID eventId() { return UUID.randomUUID(); }
            @Override public String eventName() { return "unexpected"; }
            @Override public Instant occurredOn() { return Instant.now(); }
            @Override public String correlationId() { return "correlation"; }
            @Override public String logicalDestination() { return "unexpected.destination"; }
        }));

        var unexpectedDestination = mock(com.mcalvaro.mscatering.integration.contracts.SubscriptionCreatedIntegrationEvent.class);
        when(unexpectedDestination.logicalDestination()).thenReturn("unexpected.destination");
        assertThatIllegalArgumentException().isThrownBy(() -> router.route(unexpectedDestination));
    }
}
