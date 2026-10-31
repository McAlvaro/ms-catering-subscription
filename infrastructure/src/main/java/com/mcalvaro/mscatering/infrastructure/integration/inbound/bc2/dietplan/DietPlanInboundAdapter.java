package com.mcalvaro.mscatering.infrastructure.integration.inbound.bc2.dietplan;

import an.awesome.pipelinr.Pipeline;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.mcalvaro.mscatering.application.dietplan.synchronization.SynchronizeDietPlanCommand;
import com.mcalvaro.mscatering.domain.core.DomainException;
import com.mcalvaro.mscatering.domain.core.id.DietPlanId;
import com.mcalvaro.mscatering.domain.core.id.PatientId;
import com.mcalvaro.mscatering.domain.core.id.RecipeId;
import com.mcalvaro.mscatering.domain.dietplan.*;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalTime;
import java.util.*;

/** Transport-neutral anti-corruption adapter for the two immutable BC2 plan events. */
@Component
public class DietPlanInboundAdapter {
    private static final String SOURCE = "MSNutritionPlanner";
    private final Pipeline pipeline;
    public DietPlanInboundAdapter(Pipeline pipeline) { this.pipeline = pipeline; }
    public void receiveBase(DietPlanCreatedEvent event) { receive(event, PlanType.BASE); }
    public void receivePersonalized(DietPlanCreatedEvent event) { receive(event, PlanType.PERSONALIZED); }
    private void receive(DietPlanCreatedEvent event, PlanType type) {
        validateEnvelope(event, type);
        DietPlanData data = event.data();
        DietPlanProjection plan = new DietPlanProjection(
                DietPlanId.of(data.id()),
                type,
                type == PlanType.PERSONALIZED ? PatientId.of(data.patientId()) : null,
                data.active(), data.effectiveStart(), data.durationDays(), data.days().stream().map(this::day).toList());
        pipeline.send(new SynchronizeDietPlanCommand(event.eventId(), event.eventName(), event.eventVersion(), event.occurredOn(),
                event.source(), event.correlationId(), plan, fingerprint(data, type)));
    }
    private void validateEnvelope(DietPlanCreatedEvent event, PlanType type) {
        String expected = type == PlanType.BASE ? "bc2.PlanBaseCreado" : "bc2.PlanPersonalizadoCreado";
        if (event == null || event.eventId() == null || !expected.equals(event.eventName()) || !"1.0".equals(event.eventVersion())
                || event.occurredOn() == null || !SOURCE.equals(event.source()) || blank(event.correlationId()) || event.data() == null) invalid();
        DietPlanData data = event.data();
        if (blank(data.id()) || data.effectiveStart() == null || data.durationDays() <= 0 || data.days() == null
                || (type == PlanType.PERSONALIZED && (blank(data.basePlanId()) || blank(data.patientId()) || blank(data.changeDescription())))) invalid();
    }
    private DietPlanDay day(DietPlanDayData day) {
        if (day == null || day.id() == null || day.mealCount() <= 0 || day.meals() == null) invalid();
        return new DietPlanDay(day.id(), day.dayNumber(), day.mealCount(), day.meals().stream().map(this::meal).toList());
    }
    private DietPlanMeal meal(DietPlanMealData meal) {
        if (meal == null || meal.id() == null || meal.type() == null || meal.time() == null || !meal.time().matches("(?:[01]\\d|2[0-3]):[0-5]\\d") || meal.recipes() == null) invalid();
        return new DietPlanMeal(meal.id(), meal.type(), LocalTime.parse(meal.time()), meal.recipes().stream().map(this::recipe).toList());
    }
    private DietPlanRecipe recipe(DietPlanRecipeData recipe) {
        if (recipe == null || blank(recipe.recipeId()) || blank(recipe.name()) || recipe.portionGrams() <= 0) invalid();
        return new DietPlanRecipe(RecipeId.of(recipe.recipeId()), recipe.name(), recipe.portionGrams());
    }
    private String fingerprint(DietPlanData data, PlanType type) {
        List<String> values = new ArrayList<>(List.of(type.name(), String.valueOf(data.id()), String.valueOf(data.status()), data.effectiveStart().toString(), String.valueOf(data.durationDays()), String.valueOf(data.active()), String.valueOf(data.basePlanId()), String.valueOf(data.patientId()), String.valueOf(data.changeDescription())));
        data.days().stream().sorted(Comparator.comparingInt(DietPlanDayData::dayNumber)).forEach(day -> { values.add(day.id().toString()); values.add(String.valueOf(day.dayNumber())); values.add(String.valueOf(day.mealCount())); day.meals().stream().sorted(Comparator.comparing(DietPlanMealData::time)).forEach(meal -> { values.add(meal.id().toString()); values.add(meal.type().name()); values.add(meal.time()); meal.recipes().stream().sorted(Comparator.comparing(recipe -> String.valueOf(recipe.recipeId()))).forEach(recipe -> { values.add(String.valueOf(recipe.recipeId())); values.add(recipe.name()); values.add(String.valueOf(recipe.portionGrams())); }); }); });
        return sha256(values.toArray(String[]::new));
    }
    private boolean blank(String value) { return value == null || value.isBlank(); }
    private void invalid() { throw new DomainException("PROJ-001", "Invalid BC2 diet-plan canonical contract"); }
    private String sha256(String... values) { try { MessageDigest digest = MessageDigest.getInstance("SHA-256"); for (String value : values) { byte[] bytes = value.getBytes(StandardCharsets.UTF_8); digest.update((byte)(bytes.length >>> 24)); digest.update((byte)(bytes.length >>> 16)); digest.update((byte)(bytes.length >>> 8)); digest.update((byte)bytes.length); digest.update(bytes); } return HexFormat.of().formatHex(digest.digest()); } catch (Exception e) { throw new IllegalStateException(e); } }

    public record DietPlanCreatedEvent(UUID eventId, String eventName, String eventVersion, java.time.Instant occurredOn,
            String source, String correlationId, DietPlanData data) { }
    public record DietPlanData(String id, @JsonProperty("estadoPlan") String status, @JsonProperty("fechaInicioVigencia") java.time.LocalDate effectiveStart,
            @JsonProperty("duracionDias") int durationDays, @JsonProperty("activo") boolean active, @JsonProperty("dias") List<DietPlanDayData> days,
            @JsonProperty("idPlanBase") String basePlanId, @JsonProperty("idPaciente") String patientId, @JsonProperty("descripcionCambios") String changeDescription) { }
    public record DietPlanDayData(UUID id, @JsonProperty("numOrden") int dayNumber, @JsonProperty("nroTiemposComida") int mealCount,
            @JsonProperty("tiemposDeComida") List<DietPlanMealData> meals) { }
    public record DietPlanMealData(UUID id, @JsonProperty("tipoComida") MealType type, @JsonProperty("hora") String time, @JsonProperty("recetas") List<DietPlanRecipeData> recipes) {
    }
    public record DietPlanRecipeData(@JsonProperty("idReceta") String recipeId, @JsonProperty("nombre") String name, @JsonProperty("porcionGramos") int portionGrams) { }
}
