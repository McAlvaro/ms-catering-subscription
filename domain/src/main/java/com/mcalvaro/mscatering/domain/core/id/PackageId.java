package com.mcalvaro.mscatering.domain.core.id;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.mcalvaro.mscatering.domain.core.DomainId;

import java.util.Objects;
import java.util.UUID;

public record PackageId(@JsonValue String value) implements DomainId {

    public static final String PREFIX = "PKG-";

    public PackageId {
        DomainId.validate(value, PREFIX, "PackageId");
    }

    @JsonCreator
    public static PackageId of(String value) {
        return new PackageId(value);
    }

    public static PackageId of(UUID uuid) {
        Objects.requireNonNull(uuid, "uuid must not be null");
        return new PackageId(PREFIX + uuid);
    }

    public static PackageId generate() {
        return new PackageId(PREFIX + DomainId.randomToken());
    }

    @Override
    public String toString() {
        return value;
    }
}
