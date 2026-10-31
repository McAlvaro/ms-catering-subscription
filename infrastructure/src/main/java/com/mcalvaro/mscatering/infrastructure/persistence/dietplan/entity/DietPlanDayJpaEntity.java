package com.mcalvaro.mscatering.infrastructure.persistence.dietplan.entity;

import jakarta.persistence.*;
import java.util.*;

@Entity @Table(name = "diet_plan_days")
public class DietPlanDayJpaEntity {
    @Id
    @Column(name = "id", length = 64)
    private String id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "plan_id", nullable = false) private DietPlanProjectionJpaEntity plan;
    @Column(name = "day_number", nullable = false) private int dayNumber;
    @Column(name = "meal_count", nullable = false) private int mealCount;
    @OneToMany(mappedBy = "day", cascade = CascadeType.ALL, orphanRemoval = true) private List<DietPlanMealJpaEntity> meals = new ArrayList<>();
    public String getId() { return id; } public void setId(String value) { id = value; }
    public void setPlan(DietPlanProjectionJpaEntity value) { plan = value; }
    public int getDayNumber() { return dayNumber; } public void setDayNumber(int value) { dayNumber = value; }
    public int getMealCount() { return mealCount; } public void setMealCount(int value) { mealCount = value; }
    public List<DietPlanMealJpaEntity> getMeals() { return meals; }
    public void setMeals(List<DietPlanMealJpaEntity> value) { meals.clear(); value.forEach(meal -> { meal.setDay(this); meals.add(meal); }); }
}
