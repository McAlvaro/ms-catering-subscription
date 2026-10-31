package com.mcalvaro.mscatering.domain.core.id;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.mcalvaro.mscatering.domain.core.DomainId;

import java.util.Objects;
import java.util.UUID;

public record DeliveryId(@JsonValue String value) implements DomainId {

    public static final String PREFIX = "ENT-";

    public DeliveryId {
        DomainId.validate(value, PREFIX, "DeliveryId");
    }

    @JsonCreator
    public static DeliveryId of(String value) {
        return new DeliveryId(value);
    }

    public static DeliveryId of(UUID uuid) {
        Objects.requireNonNull(uuid, "uuid must not be null");
        return new DeliveryId(PREFIX + uuid);
    }

    public static DeliveryId generate() {
        return new DeliveryId(PREFIX + UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value;
    }
}
