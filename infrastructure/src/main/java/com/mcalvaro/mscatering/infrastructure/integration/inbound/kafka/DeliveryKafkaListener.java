package com.mcalvaro.mscatering.infrastructure.integration.inbound.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mcalvaro.mscatering.domain.core.DomainException;
import com.mcalvaro.mscatering.infrastructure.integration.inbound.bc5.delivery.DeliveryConfirmedEvent;
import com.mcalvaro.mscatering.infrastructure.integration.inbound.bc5.delivery.DeliveryContractException;
import com.mcalvaro.mscatering.infrastructure.integration.inbound.bc5.delivery.DeliveryFailedEvent;
import com.mcalvaro.mscatering.infrastructure.integration.inbound.bc5.delivery.DeliveryInboundAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;

import java.util.Objects;
import java.util.Set;

/**
 * Inbound Kafka listener for BC5 Logistics delivery integration events.
 */
public class DeliveryKafkaListener {

    private static final Logger log = LoggerFactory.getLogger(DeliveryKafkaListener.class);

    public static final String CONFIRMED_EVENT = "bc5.EntregaConfirmada";
    public static final String FAILED_EVENT = "bc5.EntregaNoConcretada";
    private static final Set<String> SUPPORTED_EVENTS = Set.of(CONFIRMED_EVENT, FAILED_EVENT);

    private final DeliveryInboundAdapter inboundAdapter;
    private final ObjectMapper objectMapper;

    public DeliveryKafkaListener(DeliveryInboundAdapter inboundAdapter, ObjectMapper objectMapper) {
        this.inboundAdapter = Objects.requireNonNull(inboundAdapter, "inboundAdapter must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    public DeliveryKafkaListener(DeliveryInboundAdapter inboundAdapter) {
        this(inboundAdapter, defaultObjectMapper());
    }

    @KafkaListener(
            topics = "${app.kafka.topics.delivery:bc5.logistica.eventos}",
            groupId = "${app.kafka.consumer.delivery-group-id:ms-catering-bc5-delivery-consumer}"
    )
    public void onMessage(String payload) {
        if (payload == null || payload.isBlank()) {
            log.warn("Received empty or blank Kafka message payload on delivery events topic");
            return;
        }

        try {
            JsonNode rootNode = objectMapper.readTree(payload);
            if (rootNode == null || !rootNode.hasNonNull("eventName")) {
                log.info("Ignoring event without eventName property");
                return;
            }

            String eventName = rootNode.get("eventName").asText();
            if (!SUPPORTED_EVENTS.contains(eventName)) {
                log.info("Ignoring event with name '{}'; supported are {}", eventName, SUPPORTED_EVENTS);
                return;
            }

            if (CONFIRMED_EVENT.equals(eventName)) {
                DeliveryConfirmedEvent event = objectMapper.treeToValue(rootNode, DeliveryConfirmedEvent.class);
                inboundAdapter.receiveConfirmed(event);
            } else if (FAILED_EVENT.equals(eventName)) {
                DeliveryFailedEvent event = objectMapper.treeToValue(rootNode, DeliveryFailedEvent.class);
                inboundAdapter.receiveFailed(event);
            }
        } catch (DeliveryContractException exception) {
            log.warn("DeliveryContractException processing delivery event: code={}, message={}",
                    exception.getCode(), exception.getMessage(), exception);
        } catch (DomainException exception) {
            log.warn("DomainException processing delivery event: code={}, message={}",
                    exception.getCode(), exception.getMessage(), exception);
        } catch (JsonProcessingException exception) {
            log.error("Failed to parse Kafka message payload as delivery event: {}", payload, exception);
        } catch (Exception exception) {
            log.error("Unexpected error processing Kafka message payload: {}", payload, exception);
        }
    }

    public static ObjectMapper defaultObjectMapper() {
        return JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
    }
}
