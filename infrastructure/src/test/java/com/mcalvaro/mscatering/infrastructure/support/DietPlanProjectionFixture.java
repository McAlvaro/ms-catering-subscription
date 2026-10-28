package com.mcalvaro.mscatering.infrastructure.support;

import com.mcalvaro.mscatering.infrastructure.integration.inbound.bc2.dietplan.DietPlanInboundAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

/** Test-only setup that reaches persistence through the production BC2 adapter. */
public class DietPlanProjectionFixture {
    private final DietPlanInboundAdapter adapter;
    @Autowired public DietPlanProjectionFixture(DietPlanInboundAdapter adapter) { this.adapter = adapter; }
    public UUID ingestActiveBasePlan(LocalDate start, int durationDays) {
        UUID id = UUID.randomUUID();
        adapter.receiveBase(event(id, start, durationDays));
        return id;
    }
    public void ingestActiveBasePlan(UUID id, LocalDate start, int durationDays) { adapter.receiveBase(event(id, start, durationDays)); }
    private DietPlanInboundAdapter.DietPlanCreatedEvent event(UUID id, LocalDate start, int duration) {
        return new DietPlanInboundAdapter.DietPlanCreatedEvent(UUID.randomUUID(), "bc2.PlanBaseCreado", "1.0", Instant.now(), "MSNutritionPlanner", "test-plan-" + UUID.randomUUID(),
                new DietPlanInboundAdapter.DietPlanData(id, "ACTIVO", start, duration, true,
                        IntStream.rangeClosed(1, duration).mapToObj(day -> new DietPlanInboundAdapter.DietPlanDayData(UUID.randomUUID(), day, 1,
                                List.of(new DietPlanInboundAdapter.DietPlanMealData(UUID.randomUUID(), "12:00", List.of(new DietPlanInboundAdapter.DietPlanRecipeData(UUID.randomUUID(), "Test recipe", 100)))))).toList(),
                        null, null, null));
    }
}
