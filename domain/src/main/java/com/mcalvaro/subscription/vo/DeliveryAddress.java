package com.mcalvaro.subscription.vo;

import com.mcalvaro.core.DomainException;

/**
 * Value Object representing a full delivery address (DireccionDeEntrega).
 * Validates that {@code street} and {@code city} are non-blank.
 */
public record DeliveryAddress(
        String street,
        String number,
        String city,
        String reference,
        double latitude,
        double longitude,
        String phone) {

    public DeliveryAddress {
        if (street == null || street.isBlank()) {
            throw new DomainException("VO-003", "DeliveryAddress: street must not be blank.");
        }
        if (city == null || city.isBlank()) {
            throw new DomainException("VO-004", "DeliveryAddress: city must not be blank.");
        }
    }
}
