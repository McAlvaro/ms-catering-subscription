package com.mcalvaro.mscatering.domain.core.id;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.mcalvaro.mscatering.domain.core.DomainId;

import java.util.Objects;
import java.util.UUID;

public record ConsolidatedLineRecipeId(@JsonValue String value) implements DomainId {

    public static final String PREFIX = "CLR-";

    public ConsolidatedLineRecipeId {
        DomainId.validate(value, PREFIX, "ConsolidatedLineRecipeId");
    }

    @JsonCreator
    public static ConsolidatedLineRecipeId of(String value) {
        return new ConsolidatedLineRecipeId(value);
    }

    public static ConsolidatedLineRecipeId of(UUID uuid) {
        Objects.requireNonNull(uuid, "uuid must not be null");
        return new ConsolidatedLineRecipeId(PREFIX + uuid);
    }

    public static ConsolidatedLineRecipeId generate() {
        return new ConsolidatedLineRecipeId(PREFIX + DomainId.randomToken());
    }

    @Override
    public String toString() {
        return value;
    }
}
