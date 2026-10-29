package com.mcalvaro.mscatering.application.dietplan;

import com.mcalvaro.mscatering.domain.core.DomainException;
import com.mcalvaro.mscatering.domain.dietplan.*;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class DietPlanResolverTest {
    private final IDietPlanProjectionRepository repository = mock(IDietPlanProjectionRepository.class);
    private final DietPlanResolver resolver = new DietPlanResolver(repository);

    @Test
    void rejectsMissingInactiveWrongOwnerAndWrongPeriodPlans() {
        UUID id = UUID.randomUUID(); UUID owner = UUID.randomUUID(); LocalDate start = LocalDate.of(2026, 1, 1);
        when(repository.findById(id)).thenReturn(Optional.empty());
        assertCode("PLAN-001", () -> resolver.resolve(id, owner, start, start.plusDays(14)));
        when(repository.findById(id)).thenReturn(Optional.of(plan(id, PlanType.BASE, null, false, start)));
        assertCode("PLAN-002", () -> resolver.resolve(id, owner, start, start.plusDays(14)));
        when(repository.findById(id)).thenReturn(Optional.of(plan(id, PlanType.PERSONALIZED, UUID.randomUUID(), true, start)));
        assertCode("PLAN-003", () -> resolver.resolve(id, owner, start, start.plusDays(14)));
        when(repository.findById(id)).thenReturn(Optional.of(plan(id, PlanType.BASE, null, true, start)));
        assertCode("PLAN-004", () -> resolver.resolve(id, owner, start.plusDays(1), start.plusDays(15)));
    }
    private void assertCode(String code, Runnable operation) { assertThatThrownBy(operation::run).isInstanceOf(DomainException.class).hasFieldOrPropertyWithValue("code", code); }
    private DietPlanProjection plan(UUID id, PlanType type, UUID owner, boolean active, LocalDate start) {
        return new DietPlanProjection(id, type, owner, active, start, 15, java.util.stream.IntStream.rangeClosed(1, 15).mapToObj(day -> new DietPlanDay(UUID.randomUUID(), day, 1, List.of(new DietPlanMeal(UUID.randomUUID(), MealType.LUNCH, LocalTime.NOON, List.of(new DietPlanRecipe(UUID.randomUUID(), "Meal", 1)))))).toList());
    }
}
