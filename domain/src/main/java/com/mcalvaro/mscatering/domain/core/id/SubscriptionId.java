package com.mcalvaro.mscatering.domain.core.id;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.mcalvaro.mscatering.domain.core.DomainId;

import java.util.Objects;
import java.util.UUID;

public record SubscriptionId(@JsonValue String value) implements DomainId {

    public static final String PREFIX = "SUS-";

    public SubscriptionId {
        DomainId.validate(value, PREFIX, "SubscriptionId");
    }

    @JsonCreator
    public static SubscriptionId of(String value) {
        return new SubscriptionId(value);
    }

    public static SubscriptionId of(UUID uuid) {
        Objects.requireNonNull(uuid, "uuid must not be null");
        return new SubscriptionId(PREFIX + uuid);
    }

    public static SubscriptionId generate() {
        return new SubscriptionId(PREFIX + DomainId.randomToken());
    }

    @Override
    public String toString() {
        return value;
    }
}
