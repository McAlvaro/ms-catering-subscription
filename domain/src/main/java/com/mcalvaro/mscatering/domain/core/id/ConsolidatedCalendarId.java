package com.mcalvaro.mscatering.domain.core.id;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.mcalvaro.mscatering.domain.core.DomainId;

import java.util.Objects;
import java.util.UUID;

public record ConsolidatedCalendarId(@JsonValue String value) implements DomainId {

    public static final String PREFIX = "CAL-";

    public ConsolidatedCalendarId {
        DomainId.validate(value, PREFIX, "ConsolidatedCalendarId");
    }

    @JsonCreator
    public static ConsolidatedCalendarId of(String value) {
        return new ConsolidatedCalendarId(value);
    }

    public static ConsolidatedCalendarId of(UUID uuid) {
        Objects.requireNonNull(uuid, "uuid must not be null");
        return new ConsolidatedCalendarId(PREFIX + uuid);
    }

    public static ConsolidatedCalendarId generate() {
        return new ConsolidatedCalendarId(PREFIX + UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value;
    }
}
