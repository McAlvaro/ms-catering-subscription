package com.mcalvaro.mscatering.domain.consolidatedcalendar.entity;

import com.mcalvaro.mscatering.domain.core.DomainException;

import java.time.LocalTime;
import java.util.UUID;

/** Immutable recipe selected for one operational delivery. */
public record ConsolidatedRecipeSnapshot(UUID recipeId, String name, LocalTime time, int portionGrams) {
    public ConsolidatedRecipeSnapshot {
        if (recipeId == null || name == null || name.isBlank() || time == null || portionGrams <= 0) {
            throw new DomainException("CAL-004", "Delivery recipe snapshot is incomplete");
        }
    }
}
