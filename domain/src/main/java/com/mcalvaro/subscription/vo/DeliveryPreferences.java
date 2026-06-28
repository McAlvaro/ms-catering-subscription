package com.mcalvaro.subscription.vo;

import com.mcalvaro.core.DomainException;

/**
 * Value Object representing the patient's default delivery preferences
 * (PreferenciasDeEntrega). Used as template for each DeliveryDay on creation.
 */
public record DeliveryPreferences(
        DeliveryAddress primaryAddress,
        TimeWindow timeWindow,
        String specialInstructions) {

    public DeliveryPreferences {
        if (primaryAddress == null) {
            throw new DomainException("VO-005", "DeliveryPreferences: primaryAddress must not be null.");
        }
        if (timeWindow == null) {
            throw new DomainException("VO-006", "DeliveryPreferences: timeWindow must not be null.");
        }
    }
}
