package com.mcalvaro.mscatering.infrastructure.persistence.dietplan;

import com.mcalvaro.mscatering.domain.dietplan.*;
import com.mcalvaro.mscatering.infrastructure.persistence.dietplan.entity.*;
import org.springframework.stereotype.Component;
import java.time.LocalTime;

@Component
public class DietPlanProjectionMapper {
    public DietPlanProjectionJpaEntity toJpa(DietPlanProjection plan) {
        DietPlanProjectionJpaEntity entity = new DietPlanProjectionJpaEntity();
        entity.setId(plan.id()); entity.setPlanType(plan.type().name()); entity.setOwnerPatientId(plan.ownerPatientId());
        entity.setActive(plan.active()); entity.setEffectiveStart(plan.effectiveStart()); entity.setDurationDays(plan.durationDays());
        entity.setDays(plan.days().stream().map(this::day).toList());
        return entity;
    }
    public DietPlanProjection toDomain(DietPlanProjectionJpaEntity entity) {
        return new DietPlanProjection(entity.getId(), PlanType.valueOf(entity.getPlanType()), entity.getOwnerPatientId(),
                entity.isActive(), entity.getEffectiveStart(), entity.getDurationDays(),
                entity.getDays().stream().map(day -> new DietPlanDay(day.getId(), day.getDayNumber(), day.getMealCount(),
                        day.getMeals().stream().map(meal -> new DietPlanMeal(meal.getId(), LocalTime.parse(meal.getMealTime()),
                                meal.getRecipes().stream().map(recipe -> new DietPlanRecipe(recipe.getRecipeId(), recipe.getRecipeName(), recipe.getPortionGrams())).toList())).toList())).toList());
    }
    private DietPlanDayJpaEntity day(DietPlanDay day) {
        DietPlanDayJpaEntity entity = new DietPlanDayJpaEntity(); entity.setId(day.id()); entity.setDayNumber(day.dayNumber()); entity.setMealCount(day.mealCount());
        entity.setMeals(day.meals().stream().map(this::meal).toList()); return entity;
    }
    private DietPlanMealJpaEntity meal(DietPlanMeal meal) {
        DietPlanMealJpaEntity entity = new DietPlanMealJpaEntity(); entity.setId(meal.id()); entity.setMealTime(meal.time().toString());
        entity.setRecipes(meal.recipes().stream().map(this::recipe).toList()); return entity;
    }
    private DietPlanRecipeJpaEntity recipe(DietPlanRecipe recipe) {
        DietPlanRecipeJpaEntity entity = new DietPlanRecipeJpaEntity(); entity.setId(java.util.UUID.randomUUID()); entity.setRecipeId(recipe.recipeId()); entity.setRecipeName(recipe.name()); entity.setPortionGrams(recipe.portionGrams()); return entity;
    }
}
