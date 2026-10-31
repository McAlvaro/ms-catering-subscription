package com.mcalvaro.mscatering.domain.dietplan;

import com.mcalvaro.mscatering.domain.core.DomainException;

import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public record DietPlanMeal(String id, MealType type, LocalTime time, List<DietPlanRecipe> recipes) {
    public DietPlanMeal(MealType type, LocalTime time, List<DietPlanRecipe> recipes) {
        this(UUID.randomUUID().toString(), type, time, recipes);
    }

    public DietPlanMeal(String id, LocalTime time, List<DietPlanRecipe> recipes) {
        this(id, null, time, recipes);
    }

    public DietPlanMeal(LocalTime time, List<DietPlanRecipe> recipes) {
        this(UUID.randomUUID().toString(), null, time, recipes);
    }

    public DietPlanMeal {
        recipes = recipes == null ? List.of() : List.copyOf(recipes);
        if (id == null || id.isBlank() || time == null || recipes.isEmpty()
                || recipes.stream().map(DietPlanRecipe::recipeId).collect(Collectors.toSet()).size() != recipes.size()) {
            throw new DomainException("PROJ-001", "Diet-plan meal is invalid");
        }
    }
}
