package com.mcalvaro.mscatering.infrastructure.integration.inbound.bc2.dietplan;

import an.awesome.pipelinr.Pipeline;
import com.mcalvaro.mscatering.application.dietplan.synchronization.SynchronizeDietPlanCommand;
import com.mcalvaro.mscatering.domain.core.DomainException;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DietPlanInboundAdapterTest {
    private final Pipeline pipeline = mock(Pipeline.class);
    private final DietPlanInboundAdapter adapter = new DietPlanInboundAdapter(pipeline);

    @Test
    void mapsPersonalizedSchedulesAndIncludesIgnoredWireFieldsInItsFingerprint() {
        DietPlanInboundAdapter.DietPlanCreatedEvent event = event("bc2.PlanPersonalizadoCreado", true);
        adapter.receivePersonalized(event);
        var captured = org.mockito.ArgumentCaptor.forClass(SynchronizeDietPlanCommand.class);
        verify(pipeline).send(captured.capture());
        assertThat(captured.getValue().plan().type()).isEqualTo(com.mcalvaro.mscatering.domain.dietplan.PlanType.PERSONALIZED);
        assertThat(captured.getValue().plan().ownerPatientId().value()).isEqualTo(event.data().patientId());
        assertThat(captured.getValue().payloadFingerprint()).hasSize(64);
    }
    @Test
    void rejectsTimesThatAreNotStrictHHmm() {
        DietPlanInboundAdapter.DietPlanCreatedEvent event = event("bc2.PlanBaseCreado", false);
        var first = event.data().days().get(0);
        var invalid = new DietPlanInboundAdapter.DietPlanCreatedEvent(event.eventId(), event.eventName(), event.eventVersion(), event.occurredOn(), event.source(), event.correlationId(),
                new DietPlanInboundAdapter.DietPlanData(event.data().id(), event.data().status(), event.data().effectiveStart(), event.data().durationDays(), event.data().active(),
                         List.of(new DietPlanInboundAdapter.DietPlanDayData(first.id(), 1, 1, List.of(new DietPlanInboundAdapter.DietPlanMealData(UUID.randomUUID().toString(), com.mcalvaro.mscatering.domain.dietplan.MealType.BREAKFAST, "8:00", first.meals().get(0).recipes())))), null, null, null));
        assertThatThrownBy(() -> adapter.receiveBase(invalid)).isInstanceOf(DomainException.class).hasFieldOrPropertyWithValue("code", "PROJ-001");
    }

    @Test
    void mapsCanonicalPlanWithoutTipoComidaAndWithStringIds() {
        DietPlanInboundAdapter.DietPlanCreatedEvent event = new DietPlanInboundAdapter.DietPlanCreatedEvent(
                UUID.randomUUID(), "bc2.PlanBaseCreado", "1.0", Instant.now(), "MSNutritionPlanner", "test-gary-canonical",
                new DietPlanInboundAdapter.DietPlanData("PLAN-2001", "ACTIVO", LocalDate.of(2026, 11, 1), 1, true,
                        List.of(new DietPlanInboundAdapter.DietPlanDayData("DIA-3001", 1, 1,
                                List.of(new DietPlanInboundAdapter.DietPlanMealData("TC-9001", null, "08:00",
                                        List.of(new DietPlanInboundAdapter.DietPlanRecipeData("REC-5001", "Avena con manzana", 250)))))),
                        null, null, null));

        adapter.receiveBase(event);

        var captured = org.mockito.ArgumentCaptor.forClass(SynchronizeDietPlanCommand.class);
        verify(pipeline).send(captured.capture());
        var plan = captured.getValue().plan();
        assertThat(plan.id().value()).isEqualTo("PLAN-2001");
        assertThat(plan.days().get(0).id()).isEqualTo("DIA-3001");
        var meal = plan.days().get(0).meals().get(0);
        assertThat(meal.id()).isEqualTo("TC-9001");
        assertThat(meal.type()).isNull();
        assertThat(captured.getValue().payloadFingerprint()).hasSize(64);
    }

    private DietPlanInboundAdapter.DietPlanCreatedEvent event(String name, boolean personalized) {
        String patient = "PAC-" + UUID.randomUUID();
        String basePlanId = "PLAN-" + UUID.randomUUID();
        return new DietPlanInboundAdapter.DietPlanCreatedEvent(UUID.randomUUID(), name, "1.0", Instant.now(), "MSNutritionPlanner", "test-correlation",
                new DietPlanInboundAdapter.DietPlanData("PLAN-" + UUID.randomUUID(), "ACTIVO", LocalDate.of(2026, 1, 1), 1, true,
                         List.of(new DietPlanInboundAdapter.DietPlanDayData(UUID.randomUUID().toString(), 1, 1, List.of(new DietPlanInboundAdapter.DietPlanMealData(UUID.randomUUID().toString(), com.mcalvaro.mscatering.domain.dietplan.MealType.BREAKFAST, "08:00", List.of(new DietPlanInboundAdapter.DietPlanRecipeData("REC-" + UUID.randomUUID(), "Breakfast", 100)))))),
                        personalized ? basePlanId : null, personalized ? patient : null, personalized ? "Changed portions" : null));
    }
}
