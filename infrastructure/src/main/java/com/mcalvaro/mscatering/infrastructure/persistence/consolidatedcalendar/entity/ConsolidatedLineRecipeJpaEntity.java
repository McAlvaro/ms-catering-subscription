package com.mcalvaro.mscatering.infrastructure.persistence.consolidatedcalendar.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "consolidated_line_recipes")
public class ConsolidatedLineRecipeJpaEntity {
    @Id
    @Column(name = "id", length = 64)
    private String id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "line_id", nullable = false)
    private ConsolidatedLineJpaEntity line;
    @Column(name = "recipe_id", nullable = false, length = 64) private String recipeId;
    @Column(name = "recipe_name", nullable = false) private String recipeName;
    @Column(name = "meal_time", nullable = false) private String mealTime;
    @Column(name = "portion_grams", nullable = false) private int portionGrams;
    public String getId() { return id; } public void setId(String value) { id = value; }
    public void setLine(ConsolidatedLineJpaEntity value) { line = value; }
    public String getRecipeId() { return recipeId; } public void setRecipeId(String value) { recipeId = value; }
    public String getRecipeName() { return recipeName; } public void setRecipeName(String value) { recipeName = value; }
    public String getMealTime() { return mealTime; } public void setMealTime(String value) { mealTime = value; }
    public int getPortionGrams() { return portionGrams; } public void setPortionGrams(int value) { portionGrams = value; }
}
