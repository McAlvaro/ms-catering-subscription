package com.mcalvaro.mscatering.application.dietplan;

import com.mcalvaro.mscatering.domain.core.DomainException;
import com.mcalvaro.mscatering.domain.core.id.DietPlanId;
import com.mcalvaro.mscatering.domain.core.id.PatientId;
import com.mcalvaro.mscatering.domain.core.id.RecipeId;
import com.mcalvaro.mscatering.domain.dietplan.*;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class DietPlanResolverTest {
    private final IDietPlanProjectionRepository repository = mock(IDietPlanProjectionRepository.class);
    private final DietPlanResolver resolver = new DietPlanResolver(repository);

    @Test
    void rejectsMissingInactiveWrongOwnerAndWrongPeriodPlans() {
        DietPlanId id = DietPlanId.generate(); PatientId owner = PatientId.generate(); LocalDate start = LocalDate.of(2026, 1, 1);
        when(repository.findById(id)).thenReturn(Optional.empty());
        assertCode("PLAN-001", () -> resolver.resolve(id, owner, start, start.plusDays(14)));
        when(repository.findById(id)).thenReturn(Optional.of(plan(id, PlanType.BASE, null, false, start)));
        assertCode("PLAN-002", () -> resolver.resolve(id, owner, start, start.plusDays(14)));
        when(repository.findById(id)).thenReturn(Optional.of(plan(id, PlanType.PERSONALIZED, PatientId.generate(), true, start)));
        assertCode("PLAN-003", () -> resolver.resolve(id, owner, start, start.plusDays(14)));
        when(repository.findById(id)).thenReturn(Optional.of(plan(id, PlanType.BASE, null, true, start)));
        assertCode("PLAN-004", () -> resolver.resolve(id, owner, start.plusDays(1), start.plusDays(15)));
    }
    private void assertCode(String code, Runnable operation) { assertThatThrownBy(operation::run).isInstanceOf(DomainException.class).hasFieldOrPropertyWithValue("code", code); }
    private DietPlanProjection plan(DietPlanId id, PlanType type, PatientId owner, boolean active, LocalDate start) {
        return new DietPlanProjection(id, type, owner, active, start, 15, java.util.stream.IntStream.rangeClosed(1, 15).<DietPlanDay>mapToObj(day -> new DietPlanDay(day, 1, List.of(new DietPlanMeal(MealType.LUNCH, LocalTime.NOON, List.of(new DietPlanRecipe(RecipeId.generate(), "Meal", 1)))))).toList());
    }
}
