package com.mcalvaro.mscatering.domain.core.id;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.mcalvaro.mscatering.domain.core.DomainId;

import java.util.Objects;
import java.util.UUID;

public record PatientId(@JsonValue String value) implements DomainId {

    public static final String PREFIX = "PAC-";

    public PatientId {
        DomainId.validate(value, PREFIX, "PatientId");
    }

    @JsonCreator
    public static PatientId of(String value) {
        return new PatientId(value);
    }

    public static PatientId of(UUID uuid) {
        Objects.requireNonNull(uuid, "uuid must not be null");
        return new PatientId(PREFIX + uuid);
    }

    public static PatientId generate() {
        return new PatientId(PREFIX + UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value;
    }
}
