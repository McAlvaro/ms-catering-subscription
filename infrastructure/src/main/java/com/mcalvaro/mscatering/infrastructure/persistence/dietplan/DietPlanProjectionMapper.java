package com.mcalvaro.mscatering.infrastructure.persistence.dietplan;

import com.mcalvaro.mscatering.domain.core.id.DietPlanId;
import com.mcalvaro.mscatering.domain.core.id.PatientId;
import com.mcalvaro.mscatering.domain.core.id.RecipeId;
import com.mcalvaro.mscatering.domain.dietplan.*;
import com.mcalvaro.mscatering.infrastructure.persistence.dietplan.entity.*;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.UUID;

@Component
public class DietPlanProjectionMapper {

    public DietPlanProjectionJpaEntity toJpa(DietPlanProjection plan) {
        DietPlanProjectionJpaEntity entity = new DietPlanProjectionJpaEntity();
        entity.setId(plan.id().value());
        entity.setPlanType(plan.type().name());
        entity.setOwnerPatientId(plan.ownerPatientId() != null ? plan.ownerPatientId().value() : null);
        entity.setActive(plan.active());
        entity.setEffectiveStart(plan.effectiveStart());
        entity.setDurationDays(plan.durationDays());
        entity.setDays(plan.days().stream().map(this::day).toList());
        return entity;
    }

    public DietPlanProjection toDomain(DietPlanProjectionJpaEntity entity) {
        PatientId ownerPatientId = entity.getOwnerPatientId() != null ? PatientId.of(entity.getOwnerPatientId()) : null;
        return new DietPlanProjection(
                DietPlanId.of(entity.getId()),
                PlanType.valueOf(entity.getPlanType()),
                ownerPatientId,
                entity.isActive(),
                entity.getEffectiveStart(),
                entity.getDurationDays(),
                entity.getDays().stream().map(day -> new DietPlanDay(
                        UUID.fromString(day.getId()),
                        day.getDayNumber(),
                        day.getMealCount(),
                        day.getMeals().stream().map(meal -> new DietPlanMeal(
                                UUID.fromString(meal.getId()),
                                MealType.valueOf(meal.getMealType()),
                                LocalTime.parse(meal.getMealTime()),
                                meal.getRecipes().stream().map(recipe -> new DietPlanRecipe(
                                        RecipeId.of(recipe.getRecipeId()),
                                        recipe.getRecipeName(),
                                        recipe.getPortionGrams()
                                )).toList()
                        )).toList()
                )).toList()
        );
    }

    private DietPlanDayJpaEntity day(DietPlanDay day) {
        DietPlanDayJpaEntity entity = new DietPlanDayJpaEntity();
        entity.setId(day.id().toString());
        entity.setDayNumber(day.dayNumber());
        entity.setMealCount(day.mealCount());
        entity.setMeals(day.meals().stream().map(this::meal).toList());
        return entity;
    }

    private DietPlanMealJpaEntity meal(DietPlanMeal meal) {
        DietPlanMealJpaEntity entity = new DietPlanMealJpaEntity();
        entity.setId(meal.id().toString());
        entity.setMealType(meal.type().name());
        entity.setMealTime(meal.time().toString());
        entity.setRecipes(meal.recipes().stream().map(this::recipe).toList());
        return entity;
    }

    private DietPlanRecipeJpaEntity recipe(DietPlanRecipe recipe) {
        DietPlanRecipeJpaEntity entity = new DietPlanRecipeJpaEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setRecipeId(recipe.recipeId().value());
        entity.setRecipeName(recipe.name());
        entity.setPortionGrams(recipe.portionGrams());
        return entity;
    }
}
