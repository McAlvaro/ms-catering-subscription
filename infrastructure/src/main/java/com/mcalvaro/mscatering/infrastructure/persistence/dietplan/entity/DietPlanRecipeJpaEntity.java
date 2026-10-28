package com.mcalvaro.mscatering.infrastructure.persistence.dietplan.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity @Table(name = "diet_plan_recipes")
public class DietPlanRecipeJpaEntity {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "meal_id", nullable = false) private DietPlanMealJpaEntity meal;
    @Column(name = "recipe_id", nullable = false) private UUID recipeId;
    @Column(name = "recipe_name", nullable = false) private String recipeName;
    @Column(name = "portion_grams", nullable = false) private int portionGrams;
    public UUID getId() { return id; } public void setId(UUID value) { id = value; }
    public void setMeal(DietPlanMealJpaEntity value) { meal = value; }
    public UUID getRecipeId() { return recipeId; } public void setRecipeId(UUID value) { recipeId = value; }
    public String getRecipeName() { return recipeName; } public void setRecipeName(String value) { recipeName = value; }
    public int getPortionGrams() { return portionGrams; } public void setPortionGrams(int value) { portionGrams = value; }
}
