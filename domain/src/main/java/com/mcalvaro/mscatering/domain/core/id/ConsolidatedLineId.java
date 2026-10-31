package com.mcalvaro.mscatering.domain.core.id;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.mcalvaro.mscatering.domain.core.DomainId;

import java.util.Objects;
import java.util.UUID;

public record ConsolidatedLineId(@JsonValue String value) implements DomainId {

    public static final String PREFIX = "LINE-";

    public ConsolidatedLineId {
        DomainId.validate(value, PREFIX, "ConsolidatedLineId");
    }

    @JsonCreator
    public static ConsolidatedLineId of(String value) {
        return new ConsolidatedLineId(value);
    }

    public static ConsolidatedLineId of(UUID uuid) {
        Objects.requireNonNull(uuid, "uuid must not be null");
        return new ConsolidatedLineId(PREFIX + uuid);
    }

    public static ConsolidatedLineId generate() {
        return new ConsolidatedLineId(PREFIX + UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value;
    }
}
