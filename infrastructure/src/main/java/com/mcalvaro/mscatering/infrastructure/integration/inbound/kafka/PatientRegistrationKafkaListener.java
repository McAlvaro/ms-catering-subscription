package com.mcalvaro.mscatering.infrastructure.integration.inbound.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mcalvaro.mscatering.infrastructure.integration.inbound.bc1.patient.PatientRegisteredEvent;
import com.mcalvaro.mscatering.infrastructure.integration.inbound.bc1.patient.PatientRegistrationContractException;
import com.mcalvaro.mscatering.infrastructure.integration.inbound.bc1.patient.PatientRegistrationInboundAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;

import java.util.Objects;

/**
 * Inbound Kafka listener for BC1 patient registration events.
 */
public class PatientRegistrationKafkaListener {

    private static final Logger log = LoggerFactory.getLogger(PatientRegistrationKafkaListener.class);
    private static final String SUPPORTED_EVENT_NAME = "bc1.PacienteRegistrado";

    private final PatientRegistrationInboundAdapter inboundAdapter;
    private final ObjectMapper objectMapper;

    public PatientRegistrationKafkaListener(PatientRegistrationInboundAdapter inboundAdapter, ObjectMapper objectMapper) {
        this.inboundAdapter = Objects.requireNonNull(inboundAdapter, "inboundAdapter must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    public PatientRegistrationKafkaListener(PatientRegistrationInboundAdapter inboundAdapter) {
        this(inboundAdapter, defaultObjectMapper());
    }

    @KafkaListener(
            topics = "${app.kafka.topics.patient-registered:bc1.pacientes.eventos}",
            groupId = "${app.kafka.consumer.group-id:ms-catering-bc1-patient-consumer}"
    )
    public void onMessage(String payload) {
        if (payload == null || payload.isBlank()) {
            log.warn("Received empty or blank Kafka message payload on patient events topic");
            return;
        }

        try {
            PatientRegisteredEvent event = objectMapper.readValue(payload, PatientRegisteredEvent.class);
            if (event == null || !SUPPORTED_EVENT_NAME.equals(event.eventName())) {
                log.info("Ignoring event with name '{}'; expected '{}'",
                        event != null ? event.eventName() : null, SUPPORTED_EVENT_NAME);
                return;
            }

            inboundAdapter.receive(event);
        } catch (PatientRegistrationContractException exception) {
            log.warn("Contract validation failed for patient registration event: code={}, message={}",
                    exception.getCode(), exception.getMessage(), exception);
        } catch (JsonProcessingException exception) {
            log.error("Failed to parse Kafka message payload as PatientRegisteredEvent: {}", payload, exception);
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
