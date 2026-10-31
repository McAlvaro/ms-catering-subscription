package com.mcalvaro.mscatering.domain.core.id;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.mcalvaro.mscatering.domain.core.DomainId;

import java.util.Objects;
import java.util.UUID;

public record AddressId(@JsonValue String value) implements DomainId {

    public static final String PREFIX = "DIR-";

    public AddressId {
        DomainId.validate(value, PREFIX, "AddressId");
    }

    @JsonCreator
    public static AddressId of(String value) {
        return new AddressId(value);
    }

    public static AddressId of(UUID uuid) {
        Objects.requireNonNull(uuid, "uuid must not be null");
        return new AddressId(PREFIX + uuid);
    }

    public static AddressId generate() {
        return new AddressId(PREFIX + UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value;
    }
}
