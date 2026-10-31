package com.mcalvaro.mscatering.infrastructure.integration.outbound.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcalvaro.mscatering.application.integration.IntegrationEventPublisher;
import com.mcalvaro.mscatering.integration.contracts.IntegrationEvent;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** Kafka adapter that returns only after the broker acknowledges the record. */
public final class KafkaIntegrationEventPublisher implements IntegrationEventPublisher {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final KafkaIntegrationEventRouter router;
    private final Duration acknowledgementTimeout;

    public KafkaIntegrationEventPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper,
            KafkaIntegrationEventRouter router, Duration acknowledgementTimeout) {
        this.kafkaTemplate = Objects.requireNonNull(kafkaTemplate, "Kafka template must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "Object mapper must not be null");
        this.router = Objects.requireNonNull(router, "Kafka event router must not be null");
        if (acknowledgementTimeout == null || acknowledgementTimeout.isZero() || acknowledgementTimeout.isNegative()) {
            throw new IllegalArgumentException("Kafka acknowledgement timeout must be positive");
        }
        this.acknowledgementTimeout = acknowledgementTimeout;
    }

    @Override
    public void publish(IntegrationEvent event) {
        KafkaIntegrationEventRouter.Route route = router.route(event);
        String json = serialize(event);
        ProducerRecord<String, String> record = new ProducerRecord<>(route.topic(), route.key(), json);
        record.headers().add("eventName", event.eventName().getBytes(StandardCharsets.UTF_8));
        try {
            kafkaTemplate.send(record).get(acknowledgementTimeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (ExecutionException exception) {
            throw new KafkaIntegrationEventPublishException("Kafka publication was rejected", exception.getCause());
        } catch (TimeoutException exception) {
            throw new KafkaIntegrationEventPublishException("Kafka publication acknowledgement timed out", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new KafkaIntegrationEventPublishException("Kafka publication was interrupted", exception);
        }
    }

    private String serialize(IntegrationEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException exception) {
            throw new KafkaIntegrationEventPublishException("Could not serialize integration event", exception);
        }
    }
}
