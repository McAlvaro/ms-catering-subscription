package com.mcalvaro.mscatering.infrastructure.persistence.outbox.codec;

/** Opaque, durable representation of a domain event. */
public record EncodedDomainEvent(String type, String content) {

    public EncodedDomainEvent {
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("Domain event type must not be blank");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Domain event content must not be blank");
        }
    }
}
