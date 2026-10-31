package com.mcalvaro.mscatering.domain.core.id;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.mcalvaro.mscatering.domain.core.DomainId;

import java.util.Objects;
import java.util.UUID;

public record DietPlanId(@JsonValue String value) implements DomainId {

    public static final String PREFIX = "PLAN-";

    public DietPlanId {
        DomainId.validate(value, PREFIX, "DietPlanId");
    }

    @JsonCreator
    public static DietPlanId of(String value) {
        return new DietPlanId(value);
    }

    public static DietPlanId of(UUID uuid) {
        Objects.requireNonNull(uuid, "uuid must not be null");
        return new DietPlanId(PREFIX + uuid);
    }

    public static DietPlanId generate() {
        return new DietPlanId(PREFIX + DomainId.randomToken());
    }

    @Override
    public String toString() {
        return value;
    }
}
