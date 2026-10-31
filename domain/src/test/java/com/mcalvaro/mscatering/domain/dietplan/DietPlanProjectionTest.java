package com.mcalvaro.mscatering.domain.dietplan;

import com.mcalvaro.mscatering.domain.core.DomainException;
import com.mcalvaro.mscatering.domain.core.id.DietPlanId;
import com.mcalvaro.mscatering.domain.core.id.PatientId;
import com.mcalvaro.mscatering.domain.core.id.RecipeId;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DietPlanProjectionTest {

    @Test
    void preservesAnImmutablePersonalizedPlanSchedule() {
        PatientId patientId = PatientId.generate();
        DietPlanProjection plan = new DietPlanProjection(DietPlanId.generate(), PlanType.PERSONALIZED, patientId, true,
                LocalDate.of(2026, 2, 1), 1, List.of(day(1)));

        assertThat(plan.ownerPatient()).contains(patientId);
        assertThat(plan.days()).hasSize(1);
        assertThatThrownBy(() -> plan.days().add(day(2))).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rejectsSchedulesThatDoNotCoverEveryConsecutiveDay() {
        assertThatThrownBy(() -> new DietPlanProjection(DietPlanId.generate(), PlanType.BASE, null, true,
                LocalDate.of(2026, 2, 1), 2, List.of(day(2))))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("code", "PROJ-001");
    }

    @Test
    void rejectsMealsScheduledAtTheSameTimeOnOneDay() {
        LocalTime breakfastTime = LocalTime.of(8, 0);
        DietPlanMeal breakfast = new DietPlanMeal(UUID.randomUUID().toString(), MealType.BREAKFAST, breakfastTime,
                List.of(new DietPlanRecipe(RecipeId.generate(), "Breakfast", 250)));
        DietPlanMeal duplicateBreakfast = new DietPlanMeal(UUID.randomUUID().toString(), MealType.BREAKFAST, breakfastTime,
                List.of(new DietPlanRecipe(RecipeId.generate(), "Second breakfast", 200)));

        assertThatThrownBy(() -> new DietPlanDay(UUID.randomUUID().toString(), 1, 2, List.of(breakfast, duplicateBreakfast)))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("code", "PROJ-001");
    }

    @Test
    void allowsStringIdsAndNullableMealType() {
        DietPlanMeal meal = new DietPlanMeal("TC-9001", LocalTime.of(8, 0),
                List.of(new DietPlanRecipe(RecipeId.generate(), "Avena", 250)));
        assertThat(meal.id()).isEqualTo("TC-9001");
        assertThat(meal.type()).isNull();

        DietPlanDay day = new DietPlanDay("DIA-3001", 1, 1, List.of(meal));
        assertThat(day.id()).isEqualTo("DIA-3001");
        assertThat(day.meals()).hasSize(1);
    }

    @Test
    void rejectsBlankIds() {
        assertThatThrownBy(() -> new DietPlanMeal("  ", LocalTime.of(8, 0),
                List.of(new DietPlanRecipe(RecipeId.generate(), "Avena", 250))))
                .isInstanceOf(DomainException.class);

        DietPlanMeal validMeal = new DietPlanMeal("TC-1", LocalTime.of(8, 0),
                List.of(new DietPlanRecipe(RecipeId.generate(), "Avena", 250)));
        assertThatThrownBy(() -> new DietPlanDay("", 1, 1, List.of(validMeal)))
                .isInstanceOf(DomainException.class);
    }

    private DietPlanDay day(int number) {
        return new DietPlanDay(UUID.randomUUID().toString(), number, 1,
                List.of(new DietPlanMeal(UUID.randomUUID().toString(), MealType.BREAKFAST, LocalTime.of(8, 0),
                        List.of(new DietPlanRecipe(RecipeId.generate(), "Breakfast", 250)))));
    }
}
