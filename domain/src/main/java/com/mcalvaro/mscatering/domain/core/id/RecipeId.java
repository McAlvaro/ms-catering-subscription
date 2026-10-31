package com.mcalvaro.mscatering.domain.core.id;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.mcalvaro.mscatering.domain.core.DomainId;

import java.util.Objects;
import java.util.UUID;

public record RecipeId(@JsonValue String value) implements DomainId {

    public static final String PREFIX = "REC-";

    public RecipeId {
        DomainId.validate(value, PREFIX, "RecipeId");
    }

    @JsonCreator
    public static RecipeId of(String value) {
        return new RecipeId(value);
    }

    public static RecipeId of(UUID uuid) {
        Objects.requireNonNull(uuid, "uuid must not be null");
        return new RecipeId(PREFIX + uuid);
    }

    public static RecipeId generate() {
        return new RecipeId(PREFIX + DomainId.randomToken());
    }

    @Override
    public String toString() {
        return value;
    }
}
