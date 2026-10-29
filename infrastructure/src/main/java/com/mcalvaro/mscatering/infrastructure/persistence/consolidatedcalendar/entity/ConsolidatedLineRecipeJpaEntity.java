package com.mcalvaro.mscatering.infrastructure.persistence.consolidatedcalendar.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "consolidated_line_recipes")
public class ConsolidatedLineRecipeJpaEntity {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "line_id", nullable = false)
    private ConsolidatedLineJpaEntity line;
    @Column(name = "recipe_id", nullable = false) private UUID recipeId;
    @Column(name = "recipe_name", nullable = false) private String recipeName;
    @Column(name = "meal_time", nullable = false) private String mealTime;
    @Column(name = "portion_grams", nullable = false) private int portionGrams;
    public UUID getId() { return id; } public void setId(UUID value) { id = value; }
    public void setLine(ConsolidatedLineJpaEntity value) { line = value; }
    public UUID getRecipeId() { return recipeId; } public void setRecipeId(UUID value) { recipeId = value; }
    public String getRecipeName() { return recipeName; } public void setRecipeName(String value) { recipeName = value; }
    public String getMealTime() { return mealTime; } public void setMealTime(String value) { mealTime = value; }
    public int getPortionGrams() { return portionGrams; } public void setPortionGrams(int value) { portionGrams = value; }
}
