package com.mcalvaro.mscatering.infrastructure.persistence.outbox.codec;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mcalvaro.mscatering.domain.consolidatedcalendar.event.ConsolidatedCalendarClosed;
import com.mcalvaro.mscatering.domain.core.DomainEvent;
import com.mcalvaro.mscatering.domain.subscription.event.SubscriptionCancelled;
import com.mcalvaro.mscatering.domain.subscription.event.SubscriptionCompleted;
import com.mcalvaro.mscatering.domain.subscription.event.SubscriptionCreated;
import com.mcalvaro.mscatering.domain.subscription.event.SubscriptionPaused;
import com.mcalvaro.mscatering.domain.subscription.event.SubscriptionReactivated;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Closed-registry JSON codec for the domain events that are approved for
 * durable outbox storage. Its mapper is deliberately private and isolated from
 * MVC configuration so persisted event JSON has a stable, restrictive format.
 */
@Component
public final class JacksonDomainEventCodec {

    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
            .enable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES)
            .enable(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS)
            .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
            .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
            .enable(MapperFeature.BLOCK_UNSAFE_POLYMORPHIC_BASE_TYPES)
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .build();

    private static final Map<Class<? extends DomainEvent>, String> TYPE_BY_CLASS = Map.of(
            SubscriptionCreated.class, "subscription.created",
            SubscriptionPaused.class, "subscription.paused",
            SubscriptionReactivated.class, "subscription.reactivated",
            SubscriptionCancelled.class, "subscription.cancelled",
            SubscriptionCompleted.class, "subscription.completed",
            ConsolidatedCalendarClosed.class, "consolidated-calendar.closed");

    private static final Map<String, Class<? extends DomainEvent>> CLASS_BY_TYPE = TYPE_BY_CLASS.entrySet().stream()
            .collect(Collectors.toUnmodifiableMap(Map.Entry::getValue, Map.Entry::getKey));

    public EncodedDomainEvent encode(DomainEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Domain event must not be null");
        }

        String type = TYPE_BY_CLASS.get(event.getClass());
        if (type == null) {
            throw new IllegalArgumentException("Unsupported domain event class: " + event.getClass().getName());
        }

        try {
            return new EncodedDomainEvent(type, MAPPER.writeValueAsString(event));
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Could not encode domain event type '" + type + "'", exception);
        }
    }

    public DomainEvent decode(String type, String content) {
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("Domain event type must not be blank");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Domain event content must not be blank");
        }

        Class<? extends DomainEvent> eventClass = CLASS_BY_TYPE.get(type);
        if (eventClass == null) {
            throw new IllegalArgumentException("Unknown domain event type: " + type);
        }

        validateJsonSyntax(type, content);
        try {
            return MAPPER.readValue(content, eventClass);
        } catch (JsonParseException exception) {
            throw new IllegalArgumentException("Malformed JSON for domain event type '" + type + "'", exception);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("JSON is incompatible with domain event type '" + type + "'", exception);
        }
    }

    private static void validateJsonSyntax(String type, String content) {
        try (JsonParser parser = MAPPER.getFactory().createParser(content)) {
            MAPPER.readTree(parser);
            if (parser.nextToken() != null) {
                throw new JsonParseException(parser, "Trailing JSON token");
            }
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Malformed JSON for domain event type '" + type + "'", exception);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Malformed JSON for domain event type '" + type + "'", exception);
        }
    }
}
