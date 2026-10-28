package com.mcalvaro.mscatering.domain.dietplan;

import com.mcalvaro.mscatering.domain.core.DomainException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public record DietPlanDay(UUID id, int dayNumber, int mealCount, List<DietPlanMeal> meals) {
    public DietPlanDay {
        meals = meals == null ? List.of() : List.copyOf(meals);
        if (id == null || dayNumber <= 0 || mealCount <= 0 || mealCount != meals.size()
                || meals.stream().map(DietPlanMeal::id).collect(Collectors.toSet()).size() != meals.size()
                || meals.stream().map(DietPlanMeal::time).collect(Collectors.toSet()).size() != meals.size()) {
            throw new DomainException("PROJ-001", "Diet-plan day is invalid");
        }
    }
}
