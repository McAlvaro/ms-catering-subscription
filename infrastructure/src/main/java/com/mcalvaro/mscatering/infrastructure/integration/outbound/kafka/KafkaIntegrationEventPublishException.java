package com.mcalvaro.mscatering.infrastructure.integration.outbound.kafka;

/** Signals that Kafka did not durably acknowledge an integration event. */
public final class KafkaIntegrationEventPublishException extends RuntimeException {
    public KafkaIntegrationEventPublishException(String message, Throwable cause) {
        super(message, cause);
    }
}
