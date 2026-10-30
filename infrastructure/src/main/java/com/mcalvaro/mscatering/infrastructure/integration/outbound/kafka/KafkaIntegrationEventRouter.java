package com.mcalvaro.mscatering.infrastructure.integration.outbound.kafka;

import com.mcalvaro.mscatering.integration.contracts.ConsolidatedCalendarClosedIntegrationEvent;
import com.mcalvaro.mscatering.integration.contracts.IntegrationEvent;
import com.mcalvaro.mscatering.integration.contracts.SubscriptionCancelledIntegrationEvent;
import com.mcalvaro.mscatering.integration.contracts.SubscriptionCreatedIntegrationEvent;
import com.mcalvaro.mscatering.integration.contracts.SubscriptionPausedIntegrationEvent;
import com.mcalvaro.mscatering.integration.contracts.SubscriptionReactivatedIntegrationEvent;

import java.util.Objects;

/** Closed routing table from the approved BC3 event contracts to Kafka records. */
public final class KafkaIntegrationEventRouter {
    private final String topic;

    public KafkaIntegrationEventRouter(String topic) {
        if (topic == null || topic.isBlank()) {
            throw new IllegalArgumentException("Kafka topic must not be blank");
        }
        this.topic = topic;
    }

    public Route route(IntegrationEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Integration event must not be null");
        }
        if (event instanceof SubscriptionCreatedIntegrationEvent created) {
            validateDestination(created, SubscriptionCreatedIntegrationEvent.LOGICAL_DESTINATION);
            return routeFor(created.data().subscriptionId().toString());
        }
        if (event instanceof SubscriptionPausedIntegrationEvent paused) {
            validateDestination(paused, SubscriptionPausedIntegrationEvent.LOGICAL_DESTINATION);
            return routeFor(paused.data().subscriptionId().toString());
        }
        if (event instanceof SubscriptionReactivatedIntegrationEvent reactivated) {
            validateDestination(reactivated, SubscriptionReactivatedIntegrationEvent.LOGICAL_DESTINATION);
            return routeFor(reactivated.data().subscriptionId().toString());
        }
        if (event instanceof SubscriptionCancelledIntegrationEvent cancelled) {
            validateDestination(cancelled, SubscriptionCancelledIntegrationEvent.LOGICAL_DESTINATION);
            return routeFor(cancelled.data().subscriptionId().toString());
        }
        if (event instanceof ConsolidatedCalendarClosedIntegrationEvent closed) {
            validateDestination(closed, ConsolidatedCalendarClosedIntegrationEvent.LOGICAL_DESTINATION);
            return routeFor(closed.data().calendarId().toString());
        }
        throw new IllegalArgumentException("Unsupported integration event type: " + event.getClass().getName());
    }

    private static void validateDestination(IntegrationEvent event, String expectedDestination) {
        if (!expectedDestination.equals(event.logicalDestination())) {
            throw new IllegalArgumentException("Unexpected integration event destination: " + event.logicalDestination());
        }
    }

    private Route routeFor(String key) {
        return new Route(topic, Objects.requireNonNull(key, "Kafka record key must not be null"));
    }

    public record Route(String topic, String key) {
    }
}
