package com.mcalvaro.mscatering.domain.core.id;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.mcalvaro.mscatering.domain.core.DomainId;

import java.util.Objects;
import java.util.UUID;

public record EvaluationId(@JsonValue String value) implements DomainId {

    public static final String PREFIX = "EVAL-";

    public EvaluationId {
        DomainId.validate(value, PREFIX, "EvaluationId");
    }

    @JsonCreator
    public static EvaluationId of(String value) {
        return new EvaluationId(value);
    }

    public static EvaluationId of(UUID uuid) {
        Objects.requireNonNull(uuid, "uuid must not be null");
        return new EvaluationId(PREFIX + uuid);
    }

    public static EvaluationId generate() {
        return new EvaluationId(PREFIX + UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value;
    }
}
