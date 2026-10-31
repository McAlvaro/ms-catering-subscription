package com.mcalvaro.mscatering.infrastructure.persistence.dietplan.entity;

import jakarta.persistence.*;

@Entity @Table(name = "diet_plan_recipes")
public class DietPlanRecipeJpaEntity {
    @Id
    @Column(name = "id", length = 64)
    private String id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "meal_id", nullable = false) private DietPlanMealJpaEntity meal;
    @Column(name = "recipe_id", nullable = false, length = 64) private String recipeId;
    @Column(name = "recipe_name", nullable = false) private String recipeName;
    @Column(name = "portion_grams", nullable = false) private int portionGrams;
    public String getId() { return id; } public void setId(String value) { id = value; }
    public void setMeal(DietPlanMealJpaEntity value) { meal = value; }
    public String getRecipeId() { return recipeId; } public void setRecipeId(String value) { recipeId = value; }
    public String getRecipeName() { return recipeName; } public void setRecipeName(String value) { recipeName = value; }
    public int getPortionGrams() { return portionGrams; } public void setPortionGrams(int value) { portionGrams = value; }
}
