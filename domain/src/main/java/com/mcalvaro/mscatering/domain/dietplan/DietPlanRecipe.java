package com.mcalvaro.mscatering.domain.dietplan;

import com.mcalvaro.mscatering.domain.core.DomainException;
import com.mcalvaro.mscatering.domain.core.id.RecipeId;

public record DietPlanRecipe(RecipeId recipeId, String name, int portionGrams) {
    public DietPlanRecipe {
        if (recipeId == null || name == null || name.isBlank() || portionGrams <= 0) {
            throw new DomainException("PROJ-001", "Diet-plan recipe is invalid");
        }
    }
}
