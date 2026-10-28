package com.mcalvaro.mscatering.domain.dietplan;

import com.mcalvaro.mscatering.domain.core.DomainException;

import java.util.UUID;

public record DietPlanRecipe(UUID recipeId, String name, int portionGrams) {
    public DietPlanRecipe {
        if (recipeId == null || name == null || name.isBlank() || portionGrams <= 0) {
            throw new DomainException("PROJ-001", "Diet-plan recipe is invalid");
        }
    }
}
