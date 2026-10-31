package com.mcalvaro.mscatering.infrastructure.integration.inbound.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mcalvaro.mscatering.domain.core.DomainException;
import com.mcalvaro.mscatering.infrastructure.integration.inbound.bc2.dietplan.DietPlanInboundAdapter;
import com.mcalvaro.mscatering.infrastructure.integration.inbound.bc2.dietplan.DietPlanInboundAdapter.DietPlanCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;

import java.util.Objects;
import java.util.Set;

/**
 * Inbound Kafka listener for BC2 diet plan canonical events.
 */
public class DietPlanKafkaListener {

    private static final Logger log = LoggerFactory.getLogger(DietPlanKafkaListener.class);

    private static final String BASE_PLAN_EVENT = "bc2.PlanBaseCreado";
    private static final String PERSONALIZED_PLAN_EVENT = "bc2.PlanPersonalizadoCreado";
    private static final Set<String> SUPPORTED_EVENTS = Set.of(BASE_PLAN_EVENT, PERSONALIZED_PLAN_EVENT);

    private final DietPlanInboundAdapter inboundAdapter;
    private final ObjectMapper objectMapper;

    public DietPlanKafkaListener(DietPlanInboundAdapter inboundAdapter, ObjectMapper objectMapper) {
        this.inboundAdapter = Objects.requireNonNull(inboundAdapter, "inboundAdapter must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    public DietPlanKafkaListener(DietPlanInboundAdapter inboundAdapter) {
        this(inboundAdapter, defaultObjectMapper());
    }

    @KafkaListener(
            topics = "${app.kafka.topics.diet-plan:bc2.nutricion.eventos}",
            groupId = "${app.kafka.consumer.diet-plan-group-id:ms-catering-bc2-plan-consumer}"
    )
    public void onMessage(String payload) {
        if (payload == null || payload.isBlank()) {
            log.warn("Received empty or blank Kafka message payload on diet plan events topic");
            return;
        }

        try {
            DietPlanCreatedEvent event = objectMapper.readValue(payload, DietPlanCreatedEvent.class);
            if (event == null || event.eventName() == null || !SUPPORTED_EVENTS.contains(event.eventName())) {
                log.info("Ignoring event with name '{}'; supported are {}",
                        event != null ? event.eventName() : null, SUPPORTED_EVENTS);
                return;
            }

            if (BASE_PLAN_EVENT.equals(event.eventName())) {
                inboundAdapter.receiveBase(event);
            } else if (PERSONALIZED_PLAN_EVENT.equals(event.eventName())) {
                inboundAdapter.receivePersonalized(event);
            }
        } catch (DomainException exception) {
            log.warn("Domain exception processing diet plan event: code={}, message={}",
                    exception.getCode(), exception.getMessage(), exception);
        } catch (JsonProcessingException exception) {
            log.error("Failed to parse Kafka message payload as DietPlanCreatedEvent: {}", payload, exception);
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
