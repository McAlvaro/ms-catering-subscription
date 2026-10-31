package com.mcalvaro.mscatering.infrastructure.integration.inbound.kafka;

import com.mcalvaro.mscatering.domain.core.DomainException;
import com.mcalvaro.mscatering.domain.dietplan.MealType;
import com.mcalvaro.mscatering.infrastructure.integration.inbound.bc2.dietplan.DietPlanInboundAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class DietPlanKafkaListenerTest {

    private static final String BASE_PLAN_JSON = """
            {
              "eventId": "11111111-1111-1111-1111-111111111111",
              "eventName": "bc2.PlanBaseCreado",
              "eventVersion": "1.0",
              "occurredOn": "2026-10-27T12:00:00Z",
              "source": "MSNutritionPlanner",
              "correlationId": "corr-bc2-001",
              "data": {
                "id": "22222222-2222-2222-2222-222222222222",
                "estadoPlan": "ACTIVO",
                "fechaInicioVigencia": "2026-11-01",
                "duracionDias": 7,
                "activo": true,
                "dias": [
                  {
                    "id": "33333333-3333-3333-3333-333333333333",
                    "numOrden": 1,
                    "nroTiemposComida": 1,
                    "tiemposDeComida": [
                      {
                        "id": "44444444-4444-4444-4444-444444444444",
                        "tipoComida": "BREAKFAST",
                        "hora": "08:00",
                        "recetas": [
                          {
                            "idReceta": "55555555-5555-5555-5555-555555555555",
                            "nombre": "Avena con manzana",
                            "porcionGramos": 250
                          }
                        ]
                      }
                    ]
                  }
                ]
              }
            }
            """;

    private static final String PERSONALIZED_PLAN_JSON = """
            {
              "eventId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
              "eventName": "bc2.PlanPersonalizadoCreado",
              "eventVersion": "1.0",
              "occurredOn": "2026-10-27T12:00:00Z",
              "source": "MSNutritionPlanner",
              "correlationId": "corr-bc2-002",
              "data": {
                "id": "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
                "estadoPlan": "ACTIVO",
                "fechaInicioVigencia": "2026-11-01",
                "duracionDias": 7,
                "activo": true,
                "idPlanBase": "22222222-2222-2222-2222-222222222222",
                "idPaciente": "cccccccc-cccc-cccc-cccc-cccccccccccc",
                "descripcionCambios": "Ajuste de porciones por actividad física",
                "dias": [
                  {
                    "id": "33333333-3333-3333-3333-333333333333",
                    "numOrden": 1,
                    "nroTiemposComida": 1,
                    "tiemposDeComida": [
                      {
                        "id": "44444444-4444-4444-4444-444444444444",
                        "tipoComida": "BREAKFAST",
                        "hora": "08:00",
                        "recetas": [
                          {
                            "idReceta": "55555555-5555-5555-5555-555555555555",
                            "nombre": "Avena con manzana",
                            "porcionGramos": 300
                          }
                        ]
                      }
                    ]
                  }
                ]
              }
            }
            """;

    @Mock
    private DietPlanInboundAdapter inboundAdapter;

    private DietPlanKafkaListener listener;

    @BeforeEach
    void setUp() {
        listener = new DietPlanKafkaListener(inboundAdapter);
    }

    @Test
    @DisplayName("Consumes valid PlanBaseCreado JSON and delegates to receiveBase")
    void consumesValidBasePlanJsonAndDelegatesToReceiveBase() {
        assertThatCode(() -> listener.onMessage(BASE_PLAN_JSON)).doesNotThrowAnyException();

        ArgumentCaptor<DietPlanInboundAdapter.DietPlanCreatedEvent> captor =
                ArgumentCaptor.forClass(DietPlanInboundAdapter.DietPlanCreatedEvent.class);
        verify(inboundAdapter).receiveBase(captor.capture());
        verify(inboundAdapter, never()).receivePersonalized(any());

        DietPlanInboundAdapter.DietPlanCreatedEvent event = captor.getValue();
        assertThat(event.eventId()).isEqualTo(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        assertThat(event.eventName()).isEqualTo("bc2.PlanBaseCreado");
        assertThat(event.eventVersion()).isEqualTo("1.0");
        assertThat(event.occurredOn()).isEqualTo(Instant.parse("2026-10-27T12:00:00Z"));
        assertThat(event.source()).isEqualTo("MSNutritionPlanner");
        assertThat(event.correlationId()).isEqualTo("corr-bc2-001");

        DietPlanInboundAdapter.DietPlanData data = event.data();
        assertThat(data).isNotNull();
        assertThat(data.id()).isEqualTo("22222222-2222-2222-2222-222222222222");
        assertThat(data.status()).isEqualTo("ACTIVO");
        assertThat(data.effectiveStart()).isEqualTo(LocalDate.of(2026, 11, 1));
        assertThat(data.durationDays()).isEqualTo(7);
        assertThat(data.active()).isTrue();
        assertThat(data.basePlanId()).isNull();
        assertThat(data.patientId()).isNull();
        assertThat(data.changeDescription()).isNull();
        assertThat(data.days()).hasSize(1);

        DietPlanInboundAdapter.DietPlanDayData day = data.days().get(0);
        assertThat(day.id()).isEqualTo("33333333-3333-3333-3333-333333333333");
        assertThat(day.dayNumber()).isEqualTo(1);
        assertThat(day.mealCount()).isEqualTo(1);
        assertThat(day.meals()).hasSize(1);

        DietPlanInboundAdapter.DietPlanMealData meal = day.meals().get(0);
        assertThat(meal.id()).isEqualTo("44444444-4444-4444-4444-444444444444");
        assertThat(meal.type()).isEqualTo(MealType.BREAKFAST);
        assertThat(meal.time()).isEqualTo("08:00");
        assertThat(meal.recipes()).hasSize(1);

        DietPlanInboundAdapter.DietPlanRecipeData recipe = meal.recipes().get(0);
        assertThat(recipe.recipeId()).isEqualTo("55555555-5555-5555-5555-555555555555");
        assertThat(recipe.name()).isEqualTo("Avena con manzana");
        assertThat(recipe.portionGrams()).isEqualTo(250);
    }

    @Test
    @DisplayName("Consumes valid PlanPersonalizadoCreado JSON and delegates to receivePersonalized")
    void consumesValidPersonalizedPlanJsonAndDelegatesToReceivePersonalized() {
        assertThatCode(() -> listener.onMessage(PERSONALIZED_PLAN_JSON)).doesNotThrowAnyException();

        ArgumentCaptor<DietPlanInboundAdapter.DietPlanCreatedEvent> captor =
                ArgumentCaptor.forClass(DietPlanInboundAdapter.DietPlanCreatedEvent.class);
        verify(inboundAdapter).receivePersonalized(captor.capture());
        verify(inboundAdapter, never()).receiveBase(any());

        DietPlanInboundAdapter.DietPlanCreatedEvent event = captor.getValue();
        assertThat(event.eventId()).isEqualTo(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
        assertThat(event.eventName()).isEqualTo("bc2.PlanPersonalizadoCreado");
        assertThat(event.eventVersion()).isEqualTo("1.0");
        assertThat(event.occurredOn()).isEqualTo(Instant.parse("2026-10-27T12:00:00Z"));
        assertThat(event.source()).isEqualTo("MSNutritionPlanner");
        assertThat(event.correlationId()).isEqualTo("corr-bc2-002");

        DietPlanInboundAdapter.DietPlanData data = event.data();
        assertThat(data).isNotNull();
        assertThat(data.id()).isEqualTo("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
        assertThat(data.basePlanId()).isEqualTo("22222222-2222-2222-2222-222222222222");
        assertThat(data.patientId()).isEqualTo("cccccccc-cccc-cccc-cccc-cccccccccccc");
        assertThat(data.changeDescription()).isEqualTo("Ajuste de porciones por actividad física");
        assertThat(data.days()).hasSize(1);
    }

    @Test
    @DisplayName("Ignores event with unknown or unsupported eventName")
    void ignoresEventWhenEventNameIsUnknown() {
        String unknownEventJson = """
                {
                  "eventId": "11111111-1111-1111-1111-111111111111",
                  "eventName": "bc2.PlanEliminado",
                  "eventVersion": "1.0",
                  "occurredOn": "2026-10-27T12:00:00Z",
                  "source": "MSNutritionPlanner",
                  "correlationId": "corr-bc2-003",
                  "data": {
                    "id": "22222222-2222-2222-2222-222222222222"
                  }
                }
                """;

        assertThatCode(() -> listener.onMessage(unknownEventJson)).doesNotThrowAnyException();

        verifyNoInteractions(inboundAdapter);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t\n"})
    @DisplayName("Ignores null, empty or blank payload defensively")
    void ignoresNullOrBlankPayloadDefensively(String blankPayload) {
        assertThatCode(() -> listener.onMessage(blankPayload)).doesNotThrowAnyException();

        verifyNoInteractions(inboundAdapter);
    }

    @Test
    @DisplayName("Catches JsonProcessingException defensively on malformed JSON poison pill")
    void handlesMalformedJsonPoisonPillDefensivelyWithoutPropagatingException() {
        String poisonPill = "{ malformed_json: true, ";

        assertThatCode(() -> listener.onMessage(poisonPill)).doesNotThrowAnyException();

        verifyNoInteractions(inboundAdapter);
    }

    @Test
    @DisplayName("Catches DomainException thrown by adapter receiveBase defensively without propagating")
    void catchesDomainExceptionFromReceiveBaseDefensively() {
        doThrow(new DomainException("PROJ-001", "Contract violation"))
                .when(inboundAdapter).receiveBase(any());

        assertThatCode(() -> listener.onMessage(BASE_PLAN_JSON)).doesNotThrowAnyException();

        verify(inboundAdapter).receiveBase(any());
        verify(inboundAdapter, never()).receivePersonalized(any());
    }

    @Test
    @DisplayName("Catches DomainException thrown by adapter receivePersonalized defensively without propagating")
    void catchesDomainExceptionFromReceivePersonalizedDefensively() {
        doThrow(new DomainException("PROJ-001", "Contract violation"))
                .when(inboundAdapter).receivePersonalized(any());

        assertThatCode(() -> listener.onMessage(PERSONALIZED_PLAN_JSON)).doesNotThrowAnyException();

        verify(inboundAdapter).receivePersonalized(any());
        verify(inboundAdapter, never()).receiveBase(any());
    }

    @Test
    @DisplayName("Catches unexpected Exception thrown by adapter defensively without propagating")
    void catchesUnexpectedExceptionDefensively() {
        doThrow(new RuntimeException("Simulated unexpected failure"))
                .when(inboundAdapter).receiveBase(any());

        assertThatCode(() -> listener.onMessage(BASE_PLAN_JSON)).doesNotThrowAnyException();

        verify(inboundAdapter).receiveBase(any());
    }

    @Test
    @DisplayName("Validates constructor parameters fail on null")
    void constructorRejectsNullDependencies() {
        org.junit.jupiter.api.Assertions.assertThrows(
                NullPointerException.class,
                () -> new DietPlanKafkaListener(null, DietPlanKafkaListener.defaultObjectMapper())
        );

        org.junit.jupiter.api.Assertions.assertThrows(
                NullPointerException.class,
                () -> new DietPlanKafkaListener(inboundAdapter, null)
        );

        org.junit.jupiter.api.Assertions.assertThrows(
                NullPointerException.class,
                () -> new DietPlanKafkaListener(null)
        );
    }

    @Test
    @DisplayName("Works with custom ObjectMapper and ignores unknown properties safely")
    void worksWithCustomObjectMapperAndIgnoresUnknownProperties() {
        DietPlanKafkaListener customListener = new DietPlanKafkaListener(
                inboundAdapter,
                DietPlanKafkaListener.defaultObjectMapper()
        );

        String jsonWithUnknownFields = """
                {
                  "eventId": "11111111-1111-1111-1111-111111111111",
                  "eventName": "bc2.PlanBaseCreado",
                  "eventVersion": "1.0",
                  "occurredOn": "2026-10-27T12:00:00Z",
                  "source": "MSNutritionPlanner",
                  "correlationId": "corr-bc2-001",
                  "extraField": "should-be-ignored",
                  "data": {
                    "id": "22222222-2222-2222-2222-222222222222",
                    "estadoPlan": "ACTIVO",
                    "fechaInicioVigencia": "2026-11-01",
                    "duracionDias": 7,
                    "activo": true,
                    "unmappedProperty": 12345,
                    "dias": [
                      {
                        "id": "33333333-3333-3333-3333-333333333333",
                        "numOrden": 1,
                        "nroTiemposComida": 1,
                        "tiemposDeComida": [
                          {
                            "id": "44444444-4444-4444-4444-444444444444",
                            "tipoComida": "BREAKFAST",
                            "hora": "08:00",
                            "recetas": [
                              {
                                "idReceta": "55555555-5555-5555-5555-555555555555",
                                "nombre": "Avena con manzana",
                                "porcionGramos": 250
                              }
                            ]
                          }
                        ]
                      }
                    ]
                  }
                }
                """;

        assertThatCode(() -> customListener.onMessage(jsonWithUnknownFields)).doesNotThrowAnyException();
        verify(inboundAdapter).receiveBase(any());
    }

    @Test
    @DisplayName("Consumes canonical PlanBaseCreado without tipoComida and with string IDs DIA-3001 and TC-9001")
    void consumesCanonicalGaryPlanJsonWithoutTipoComidaAndWithStringIds() {
        String canonicalGaryJson = """
                {
                  "eventId": "11111111-1111-1111-1111-111111111111",
                  "eventName": "bc2.PlanBaseCreado",
                  "eventVersion": "1.0",
                  "occurredOn": "2026-10-27T12:00:00Z",
                  "source": "MSNutritionPlanner",
                  "correlationId": "corr-bc2-001",
                  "data": {
                    "id": "PLAN-2001",
                    "estadoPlan": "ACTIVO",
                    "fechaInicioVigencia": "2026-11-01",
                    "duracionDias": 1,
                    "activo": true,
                    "dias": [
                      {
                        "id": "DIA-3001",
                        "numOrden": 1,
                        "nroTiemposComida": 1,
                        "tiemposDeComida": [
                          {
                            "id": "TC-9001",
                            "hora": "08:00",
                            "recetas": [
                              {
                                "idReceta": "REC-5001",
                                "nombre": "Avena con manzana",
                                "porcionGramos": 250
                              }
                            ]
                          }
                        ]
                      }
                    ]
                  }
                }
                """;

        assertThatCode(() -> listener.onMessage(canonicalGaryJson)).doesNotThrowAnyException();

        ArgumentCaptor<DietPlanInboundAdapter.DietPlanCreatedEvent> captor =
                ArgumentCaptor.forClass(DietPlanInboundAdapter.DietPlanCreatedEvent.class);
        verify(inboundAdapter).receiveBase(captor.capture());

        DietPlanInboundAdapter.DietPlanCreatedEvent event = captor.getValue();
        DietPlanInboundAdapter.DietPlanData data = event.data();
        assertThat(data.id()).isEqualTo("PLAN-2001");
        DietPlanInboundAdapter.DietPlanDayData day = data.days().get(0);
        assertThat(day.id()).isEqualTo("DIA-3001");
        DietPlanInboundAdapter.DietPlanMealData meal = day.meals().get(0);
        assertThat(meal.id()).isEqualTo("TC-9001");
        assertThat(meal.type()).isNull();
    }
}
