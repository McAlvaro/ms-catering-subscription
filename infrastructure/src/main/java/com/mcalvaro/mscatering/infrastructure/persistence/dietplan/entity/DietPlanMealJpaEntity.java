package com.mcalvaro.mscatering.infrastructure.persistence.dietplan.entity;

import jakarta.persistence.*;
import java.util.*;

@Entity @Table(name = "diet_plan_meals")
public class DietPlanMealJpaEntity {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "day_id", nullable = false) private DietPlanDayJpaEntity day;
    @Column(name = "meal_time", nullable = false) private String mealTime;
    @OneToMany(mappedBy = "meal", cascade = CascadeType.ALL, orphanRemoval = true) private List<DietPlanRecipeJpaEntity> recipes = new ArrayList<>();
    public UUID getId() { return id; } public void setId(UUID value) { id = value; }
    public void setDay(DietPlanDayJpaEntity value) { day = value; }
    public String getMealTime() { return mealTime; } public void setMealTime(String value) { mealTime = value; }
    public List<DietPlanRecipeJpaEntity> getRecipes() { return recipes; }
    public void setRecipes(List<DietPlanRecipeJpaEntity> value) { recipes.clear(); value.forEach(recipe -> { recipe.setMeal(this); recipes.add(recipe); }); }
}
