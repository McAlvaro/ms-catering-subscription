package com.mcalvaro.mscatering.domain.core.id;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.mcalvaro.mscatering.domain.core.DomainId;

import java.util.Objects;
import java.util.UUID;

public record PauseRequestId(@JsonValue String value) implements DomainId {

    public static final String PREFIX = "PAU-";

    public PauseRequestId {
        DomainId.validate(value, PREFIX, "PauseRequestId");
    }

    @JsonCreator
    public static PauseRequestId of(String value) {
        return new PauseRequestId(value);
    }

    public static PauseRequestId of(UUID uuid) {
        Objects.requireNonNull(uuid, "uuid must not be null");
        return new PauseRequestId(PREFIX + uuid);
    }

    public static PauseRequestId generate() {
        return new PauseRequestId(PREFIX + UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value;
    }
}
