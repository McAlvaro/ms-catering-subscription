package com.mcalvaro.mscatering.domain.core.id;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.mcalvaro.mscatering.domain.core.DomainId;

import java.util.Objects;
import java.util.UUID;

public record DeliveryCalendarId(@JsonValue String value) implements DomainId {

    public static final String PREFIX = "DCAL-";

    public DeliveryCalendarId {
        DomainId.validate(value, PREFIX, "DeliveryCalendarId");
    }

    @JsonCreator
    public static DeliveryCalendarId of(String value) {
        return new DeliveryCalendarId(value);
    }

    public static DeliveryCalendarId of(UUID uuid) {
        Objects.requireNonNull(uuid, "uuid must not be null");
        return new DeliveryCalendarId(PREFIX + uuid);
    }

    public static DeliveryCalendarId generate() {
        return new DeliveryCalendarId(PREFIX + DomainId.randomToken());
    }

    @Override
    public String toString() {
        return value;
    }
}
