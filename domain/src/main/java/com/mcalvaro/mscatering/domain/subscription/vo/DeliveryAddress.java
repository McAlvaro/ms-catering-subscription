package com.mcalvaro.mscatering.domain.subscription.vo;

import com.mcalvaro.mscatering.domain.core.DomainException;
import com.mcalvaro.mscatering.domain.core.id.AddressId;

/**
 * Value Object representing a full delivery address (DireccionDeEntrega).
 * Validates that {@code street} and {@code city} are non-blank.
 */
public record DeliveryAddress(
        AddressId addressId,
        String alias,
        String street,
        String number,
        String city,
        String reference,
        double latitude,
        double longitude,
        String phone) {

    public DeliveryAddress(String street, String number, String city, String reference,
            double latitude, double longitude, String phone) {
        this(null, null, street, number, city, reference, latitude, longitude, phone);
    }

    public DeliveryAddress(AddressId addressId, String street, String number, String city, String reference,
            double latitude, double longitude, String phone) {
        this(addressId, null, street, number, city, reference, latitude, longitude, phone);
    }

    public DeliveryAddress {
        if (street == null || street.isBlank()) {
            throw new DomainException("VO-003", "DeliveryAddress: street must not be blank.");
        }
        if (city == null || city.isBlank()) {
            throw new DomainException("VO-004", "DeliveryAddress: city must not be blank.");
        }
    }
}
