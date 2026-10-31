package com.mcalvaro.mscatering.domain.core.id;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.mcalvaro.mscatering.domain.core.DomainId;

import java.util.Objects;
import java.util.UUID;

public record DeliveryDayId(@JsonValue String value) implements DomainId {

    public static final String PREFIX = "CD-";

    public DeliveryDayId {
        DomainId.validate(value, PREFIX, "DeliveryDayId");
    }

    @JsonCreator
    public static DeliveryDayId of(String value) {
        return new DeliveryDayId(value);
    }

    public static DeliveryDayId of(UUID uuid) {
        Objects.requireNonNull(uuid, "uuid must not be null");
        return new DeliveryDayId(PREFIX + uuid);
    }

    public static DeliveryDayId generate() {
        return new DeliveryDayId(PREFIX + UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value;
    }
}
