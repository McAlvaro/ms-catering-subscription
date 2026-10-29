package com.mcalvaro.mscatering.application.outbox.translation;

import com.mcalvaro.mscatering.domain.consolidatedcalendar.entity.ConsolidatedDeliverySnapshot;
import com.mcalvaro.mscatering.domain.consolidatedcalendar.entity.ConsolidatedRecipeSnapshot;
import com.mcalvaro.mscatering.domain.consolidatedcalendar.event.ConsolidatedCalendarClosed;
import com.mcalvaro.mscatering.domain.core.DomainEvent;
import com.mcalvaro.mscatering.domain.dietplan.PlanType;
import com.mcalvaro.mscatering.domain.subscription.event.SubscriptionCancelled;
import com.mcalvaro.mscatering.domain.subscription.event.SubscriptionCompleted;
import com.mcalvaro.mscatering.domain.subscription.event.SubscriptionCreated;
import com.mcalvaro.mscatering.domain.subscription.event.SubscriptionPaused;
import com.mcalvaro.mscatering.domain.subscription.event.SubscriptionReactivated;
import com.mcalvaro.mscatering.domain.subscription.vo.ContractCode;
import com.mcalvaro.mscatering.domain.subscription.vo.DeliveryAddress;
import com.mcalvaro.mscatering.domain.subscription.vo.PauseRange;
import com.mcalvaro.mscatering.integration.contracts.ConsolidatedCalendarClosedIntegrationEvent;
import com.mcalvaro.mscatering.integration.contracts.IntegrationEvent;
import com.mcalvaro.mscatering.integration.contracts.SubscriptionCancelledIntegrationEvent;
import com.mcalvaro.mscatering.integration.contracts.SubscriptionCreatedIntegrationEvent;
import com.mcalvaro.mscatering.integration.contracts.SubscriptionPausedIntegrationEvent;
import com.mcalvaro.mscatering.integration.contracts.SubscriptionReactivatedIntegrationEvent;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OutboxEventTranslationTest {
    private static final Instant OCCURRED_AT = Instant.parse("2026-10-29T12:00:00Z");
    private static final UUID EVENT_ID = uuid(1);
    private static final UUID SUBSCRIPTION_ID = uuid(2);
    private static final UUID PATIENT_ID = uuid(3);
    private static final UUID PLAN_ID = uuid(4);
    private static final UUID CALENDAR_ID = uuid(5);
    private static final UUID FIRST_CALENDAR_DAY_ID = uuid(11);
    private static final UUID SECOND_CALENDAR_DAY_ID = uuid(12);
    private static final UUID SECOND_SUBSCRIPTION_ID = uuid(13);
    private static final UUID SECOND_PATIENT_ID = uuid(14);
    private static final UUID SECOND_PLAN_ID = uuid(15);
    private static final UUID FIRST_ADDRESS_ID = uuid(16);
    private static final UUID SECOND_ADDRESS_ID = uuid(17);
    private static final UUID BREAKFAST_RECIPE_ID = uuid(18);
    private static final UUID LUNCH_RECIPE_ID = uuid(19);
    private static final UUID DINNER_RECIPE_ID = uuid(20);
    private static final UUID SNACK_RECIPE_ID = uuid(21);

    @Test
    void mapsCreationSnapshotWithPersonalizedAndBasePlanTypes() {
        SubscriptionCreated personalized = createdEvent(PlanType.PERSONALIZED, "flow-personalized",
                List.of(uuid(30), uuid(31)), new BigDecimal("280.50"));
        SubscriptionCreatedIntegrationEvent personalizedResult =
                new SubscriptionCreatedOutboxEventTranslator().translate(personalized);

        assertCommonEnvelope(personalizedResult, EVENT_ID, OCCURRED_AT, "flow-personalized",
                "bc3.SuscripcionCreada", "bc3.suscripcion.creada.v1");
        assertCreatedData(personalizedResult, "PERSONALIZADO", 2, new BigDecimal("280.50"));

        SubscriptionCreated base = createdEvent(PlanType.BASE, "flow-base", List.of(), BigDecimal.ONE);
        SubscriptionCreatedIntegrationEvent baseResult = new SubscriptionCreatedOutboxEventTranslator().translate(base);

        assertCommonEnvelope(baseResult, EVENT_ID, OCCURRED_AT, "flow-base",
                "bc3.SuscripcionCreada", "bc3.suscripcion.creada.v1");
        assertCreatedData(baseResult, "BASE", 0, BigDecimal.ONE);
    }

    @Test
    void mapsPauseSnapshotAndCompleteEnvelope() {
        SubscriptionPaused paused = new SubscriptionPaused(EVENT_ID, OCCURRED_AT, SUBSCRIPTION_ID, PATIENT_ID,
                new PauseRange(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 8)), "Viaje",
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 11, 2), "flow-pause");
        SubscriptionPausedIntegrationEvent result = new SubscriptionPausedOutboxEventTranslator().translate(paused);

        assertCommonEnvelope(result, EVENT_ID, OCCURRED_AT, "flow-pause",
                "bc3.SuscripcionPausada", "bc3.suscripcion.pausada.v1");
        assertThat(result.data().subscriptionId()).isEqualTo(SUBSCRIPTION_ID);
        assertThat(result.data().patientId()).isEqualTo(PATIENT_ID);
        assertThat(result.data().startDate()).isEqualTo(LocalDate.of(2026, 10, 5));
        assertThat(result.data().endDate()).isEqualTo(LocalDate.of(2026, 10, 8));
        assertThat(result.data().reason()).isEqualTo("Viaje");
        assertThat(result.data().newEndDate()).isEqualTo(LocalDate.of(2026, 11, 2));
        assertThat(result.data().requestedOn()).isEqualTo(LocalDate.of(2026, 10, 1));
    }

    @Test
    void mapsReactivationSnapshotAndCompleteEnvelope() {
        SubscriptionReactivated reactivated = new SubscriptionReactivated(EVENT_ID, OCCURRED_AT, SUBSCRIPTION_ID,
                PATIENT_ID, LocalDate.of(2026, 10, 8), LocalDate.of(2026, 11, 2), "flow-reactivation");
        SubscriptionReactivatedIntegrationEvent result =
                new SubscriptionReactivatedOutboxEventTranslator().translate(reactivated);

        assertCommonEnvelope(result, EVENT_ID, OCCURRED_AT, "flow-reactivation",
                "bc3.SuscripcionReactivada", "bc3.suscripcion.reactivada.v1");
        assertThat(result.data().subscriptionId()).isEqualTo(SUBSCRIPTION_ID);
        assertThat(result.data().patientId()).isEqualTo(PATIENT_ID);
        assertThat(result.data().reactivatedOn()).isEqualTo(LocalDate.of(2026, 10, 8));
        assertThat(result.data().endDate()).isEqualTo(LocalDate.of(2026, 11, 2));
    }

    @Test
    void mapsManualCancellationAndCompletionSnapshotsWithCompleteEnvelopes() {
        SubscriptionCancelled cancelled = new SubscriptionCancelled(EVENT_ID, OCCURRED_AT, SUBSCRIPTION_ID, PATIENT_ID,
                PLAN_ID, "Solicitud del paciente", LocalDate.of(2026, 10, 9), 3, "flow-cancelled");
        SubscriptionCancelledIntegrationEvent manual = new SubscriptionCancelledOutboxEventTranslator().translate(cancelled);

        assertCommonEnvelope(manual, EVENT_ID, OCCURRED_AT, "flow-cancelled",
                "bc3.SuscripcionCancelada", "bc3.suscripcion.cancelada.v1");
        assertTerminationData(manual, "MANUAL", "Solicitud del paciente", LocalDate.of(2026, 10, 9), 3);

        SubscriptionCompleted completed = new SubscriptionCompleted(EVENT_ID, OCCURRED_AT, SUBSCRIPTION_ID, PATIENT_ID,
                PLAN_ID, LocalDate.of(2026, 10, 30), 1, "flow-completed");
        SubscriptionCancelledIntegrationEvent expiration =
                new SubscriptionCompletedOutboxEventTranslator().translate(completed);

        assertCommonEnvelope(expiration, EVENT_ID, OCCURRED_AT, "flow-completed",
                "bc3.SuscripcionCancelada", "bc3.suscripcion.cancelada.v1");
        assertTerminationData(expiration, "VENCIMIENTO", "Finalizacion del periodo contratado",
                LocalDate.of(2026, 10, 30), 1);
    }

    @Test
    void mapsCalendarSnapshotWithOrderedDeliveriesRecipesAndCompleteAddresses() {
        ConsolidatedCalendarClosed calendar = calendarClosedEvent();
        ConsolidatedCalendarClosedIntegrationEvent result =
                new ConsolidatedCalendarClosedOutboxEventTranslator().translate(calendar);

        assertCommonEnvelope(result, EVENT_ID, OCCURRED_AT, "flow-calendar",
                "bc3.CalendarioConsolidadoCerrado", "bc3.calendario.consolidado.cerrado.v1");
        assertThat(result.data().calendarId()).isEqualTo(CALENDAR_ID);
        assertThat(result.data().deliveryDate()).isEqualTo(LocalDate.of(2026, 10, 29));
        assertThat(result.data().closedOn()).isEqualTo(OCCURRED_AT);
        assertThat(result.data().totalDeliveries()).isEqualTo(2);
        assertThat(result.data().deliveries()).extracting(ConsolidatedCalendarClosedIntegrationEvent.Delivery::calendarDayId)
                .containsExactly(FIRST_CALENDAR_DAY_ID, SECOND_CALENDAR_DAY_ID);

        ConsolidatedCalendarClosedIntegrationEvent.Delivery first = result.data().deliveries().get(0);
        assertDelivery(first, FIRST_CALENDAR_DAY_ID, SUBSCRIPTION_ID, PATIENT_ID, "Ana Perez", PLAN_ID, 1);
        assertAddress(first.address(), FIRST_ADDRESS_ID, "Casa", "Av. Cristo Redentor", "Torre Norte", "Santa Cruz",
                -17.78, -63.18);
        assertThat(first.recipes()).extracting(ConsolidatedCalendarClosedIntegrationEvent.Recipe::recipeId)
                .containsExactly(BREAKFAST_RECIPE_ID, LUNCH_RECIPE_ID);
        assertRecipe(first.recipes().get(0), BREAKFAST_RECIPE_ID, "Desayuno", "08:05", new BigDecimal("250"));
        assertRecipe(first.recipes().get(1), LUNCH_RECIPE_ID, "Almuerzo", "13:45", new BigDecimal("350"));

        ConsolidatedCalendarClosedIntegrationEvent.Delivery second = result.data().deliveries().get(1);
        assertDelivery(second, SECOND_CALENDAR_DAY_ID, SECOND_SUBSCRIPTION_ID, SECOND_PATIENT_ID, "Beatriz Flores",
                SECOND_PLAN_ID, 2);
        assertAddress(second.address(), SECOND_ADDRESS_ID, "Oficina", "Calle 7 123", "Edificio Sur", "Cochabamba",
                -17.39, -66.16);
        assertThat(second.recipes()).extracting(ConsolidatedCalendarClosedIntegrationEvent.Recipe::recipeId)
                .containsExactly(DINNER_RECIPE_ID, SNACK_RECIPE_ID);
        assertRecipe(second.recipes().get(0), DINNER_RECIPE_ID, "Cena", "19:30", new BigDecimal("400"));
        assertRecipe(second.recipes().get(1), SNACK_RECIPE_ID, "Merienda", "16:15", new BigDecimal("150"));
    }

    @Test
    void registryRejectsInvalidConfigurationAndPreservesExactDispatchAfterCallerListMutation() {
        assertThatThrownBy(() -> new OutboxEventTranslatorRegistry(null))
                .isExactlyInstanceOf(IntegrationEventTranslationException.class)
                .hasMessage("Translators must not be null");

        List<OutboxEventTranslator<? extends DomainEvent>> nullElement = new ArrayList<>();
        nullElement.add(null);
        assertThatThrownBy(() -> new OutboxEventTranslatorRegistry(nullElement))
                .isExactlyInstanceOf(IntegrationEventTranslationException.class)
                .hasMessage("Translator and source type must not be null");

        OutboxEventTranslator<DomainEvent> nullSourceType = new OutboxEventTranslator<>() {
            @Override public Class<DomainEvent> sourceType() { return null; }
            @Override public IntegrationEvent translate(DomainEvent source) { throw new AssertionError("must not translate"); }
        };
        assertThatThrownBy(() -> new OutboxEventTranslatorRegistry(List.of(nullSourceType)))
                .isExactlyInstanceOf(IntegrationEventTranslationException.class)
                .hasMessage("Translator and source type must not be null");

        List<OutboxEventTranslator<? extends DomainEvent>> mutableTranslators =
                new ArrayList<>(List.of(new SubscriptionCreatedOutboxEventTranslator()));
        OutboxEventTranslatorRegistry registry = new OutboxEventTranslatorRegistry(mutableTranslators);
        mutableTranslators.clear();
        assertThat(registry.translate(createdEvent(PlanType.BASE, "flow-registry", List.of(), BigDecimal.ONE)))
                .isInstanceOf(SubscriptionCreatedIntegrationEvent.class);

        assertThatThrownBy(() -> registry.translate(null))
                .isExactlyInstanceOf(IntegrationEventTranslationException.class)
                .hasMessage("Domain event must not be null");
        assertThatThrownBy(() -> registry.translate(new UnsupportedEvent(EVENT_ID, OCCURRED_AT)))
                .isExactlyInstanceOf(IntegrationEventTranslationException.class)
                .hasMessage("Unsupported domain event type: " + UnsupportedEvent.class.getName());
        assertThatThrownBy(() -> new OutboxEventTranslatorRegistry(List.of(
                new SubscriptionCreatedOutboxEventTranslator(), new SubscriptionCreatedOutboxEventTranslator())))
                .isExactlyInstanceOf(IntegrationEventTranslationException.class)
                .hasMessage("Duplicate translator source type: " + SubscriptionCreated.class.getName());
    }

    @Test
    void registryRejectsBroadDomainEventTranslatorForSubscriptionCreatedWithoutInvokingIt() {
        AtomicInteger invocations = new AtomicInteger();
        OutboxEventTranslator<DomainEvent> broadTranslator = new OutboxEventTranslator<>() {
            @Override public Class<DomainEvent> sourceType() { return DomainEvent.class; }

            @Override public IntegrationEvent translate(DomainEvent source) {
                invocations.incrementAndGet();
                return null;
            }
        };
        OutboxEventTranslatorRegistry registry = new OutboxEventTranslatorRegistry(List.of(broadTranslator));

        assertThatThrownBy(() -> registry.translate(createdEvent(PlanType.BASE, "flow-exact-type", List.of(), BigDecimal.ONE)))
                .isExactlyInstanceOf(IntegrationEventTranslationException.class)
                .hasMessage("Unsupported domain event type: " + SubscriptionCreated.class.getName());
        assertThat(invocations).hasValue(0);
    }

    @Test
    void concreteTranslatorsRejectNullSourcesWithStableContextualMessages() {
        assertNullSource("SubscriptionCreated must not be null",
                () -> new SubscriptionCreatedOutboxEventTranslator().translate(null));
        assertNullSource("SubscriptionPaused must not be null",
                () -> new SubscriptionPausedOutboxEventTranslator().translate(null));
        assertNullSource("SubscriptionReactivated must not be null",
                () -> new SubscriptionReactivatedOutboxEventTranslator().translate(null));
        assertNullSource("SubscriptionCancelled must not be null",
                () -> new SubscriptionCancelledOutboxEventTranslator().translate(null));
        assertNullSource("SubscriptionCompleted must not be null",
                () -> new SubscriptionCompletedOutboxEventTranslator().translate(null));
        assertNullSource("ConsolidatedCalendarClosed must not be null",
                () -> new ConsolidatedCalendarClosedOutboxEventTranslator().translate(null));
    }

    @Test
    void out006TranslationAndPublisherPortProductionSourcesDoNotPublishOrGenerateRuntimeValues() throws IOException {
        List<Path> sourceFiles = out006ProductionSourceFiles();
        assertThat(sourceFiles).extracting(path -> path.getFileName().toString())
                .contains("OutboxEventTranslatorRegistry.java", "IntegrationEventPublisher.java");

        String productionSources = sourceFiles.stream()
                .map(OutboxEventTranslationTest::readSource)
                .reduce("", String::concat);

        assertThat(productionSources).doesNotContain(
                ".publish(",
                "LocalDate.now()",
                "Instant.now()",
                "Clock.system",
                "System.currentTimeMillis",
                "UUID.randomUUID");
    }

    private static void assertCreatedData(SubscriptionCreatedIntegrationEvent event, String planType,
            int evaluationCount, BigDecimal amount) {
        assertThat(event.data().subscriptionId()).isEqualTo(SUBSCRIPTION_ID);
        assertThat(event.data().patientId()).isEqualTo(PATIENT_ID);
        assertThat(event.data().planId()).isEqualTo(PLAN_ID);
        assertThat(event.data().planType()).isEqualTo(planType);
        assertThat(event.data().durationDays()).isEqualTo(30);
        assertThat(event.data().startDate()).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(event.data().endDate()).isEqualTo(LocalDate.of(2026, 10, 30));
        assertThat(event.data().includedEvaluations()).isEqualTo(evaluationCount);
        assertThat(event.data().invoicedAmount()).isEqualByComparingTo(amount);
        assertThat(event.data().currency()).isEqualTo("BOB");
        assertThat(event.data().invoiceNumber()).isNull();
    }

    private static void assertTerminationData(SubscriptionCancelledIntegrationEvent event, String type, String reason,
            LocalDate effectiveDate, int cancelledPendingDeliveries) {
        assertThat(event.data().subscriptionId()).isEqualTo(SUBSCRIPTION_ID);
        assertThat(event.data().patientId()).isEqualTo(PATIENT_ID);
        assertThat(event.data().planId()).isEqualTo(PLAN_ID);
        assertThat(event.data().terminationType()).isEqualTo(type);
        assertThat(event.data().reason()).isEqualTo(reason);
        assertThat(event.data().effectiveDate()).isEqualTo(effectiveDate);
        assertThat(event.data().cancelledPendingDeliveries()).isEqualTo(cancelledPendingDeliveries);
    }

    private static void assertDelivery(ConsolidatedCalendarClosedIntegrationEvent.Delivery delivery, UUID calendarDayId,
            UUID subscriptionId, UUID patientId, String patientName, UUID planId, int planDayNumber) {
        assertThat(delivery.calendarDayId()).isEqualTo(calendarDayId);
        assertThat(delivery.subscriptionId()).isEqualTo(subscriptionId);
        assertThat(delivery.patientId()).isEqualTo(patientId);
        assertThat(delivery.patientName()).isEqualTo(patientName);
        assertThat(delivery.planId()).isEqualTo(planId);
        assertThat(delivery.planDayNumber()).isEqualTo(planDayNumber);
    }

    private static void assertAddress(ConsolidatedCalendarClosedIntegrationEvent.Address address, UUID addressId,
            String alias, String addressText, String reference, String city, double latitude, double longitude) {
        assertThat(address.addressId()).isEqualTo(addressId);
        assertThat(address.alias()).isEqualTo(alias);
        assertThat(address.addressText()).isEqualTo(addressText);
        assertThat(address.reference()).isEqualTo(reference);
        assertThat(address.city()).isEqualTo(city);
        assertThat(address.latitude()).isEqualTo(latitude);
        assertThat(address.longitude()).isEqualTo(longitude);
    }

    private static void assertRecipe(ConsolidatedCalendarClosedIntegrationEvent.Recipe recipe, UUID recipeId,
            String name, String time, BigDecimal portionGrams) {
        assertThat(recipe.recipeId()).isEqualTo(recipeId);
        assertThat(recipe.name()).isEqualTo(name);
        assertThat(recipe.time()).isEqualTo(time);
        assertThat(recipe.portionGrams()).isEqualByComparingTo(portionGrams);
    }

    private static void assertCommonEnvelope(IntegrationEvent event, UUID eventId, Instant occurredOn,
            String correlationId, String eventName, String logicalDestination) {
        assertThat(event.eventId()).isEqualTo(eventId);
        assertThat(event.occurredOn()).isEqualTo(occurredOn);
        assertThat(event.correlationId()).isEqualTo(correlationId);
        assertThat(event.eventName()).isEqualTo(eventName);
        assertThat(event.eventVersion()).isEqualTo("1.0");
        assertThat(event.source()).isEqualTo("MsCateringSubscription");
        assertThat(event.logicalDestination()).isEqualTo(logicalDestination);
    }

    private static void assertNullSource(String expectedMessage, Runnable translation) {
        assertThatThrownBy(translation::run)
                .isExactlyInstanceOf(IntegrationEventTranslationException.class)
                .hasMessage(expectedMessage);
    }

    private static List<Path> out006ProductionSourceFiles() throws IOException {
        List<Path> sourceFiles = new ArrayList<>();
        for (Path sourceRoot : List.of(
                Path.of("src/main/java/com/mcalvaro/mscatering/application/outbox/translation"),
                Path.of("src/main/java/com/mcalvaro/mscatering/application/integration"))) {
            try (Stream<Path> paths = Files.walk(sourceRoot)) {
                sourceFiles.addAll(paths.filter(path -> path.getFileName().toString().endsWith(".java")).toList());
            }
        }
        return sourceFiles;
    }

    private static String readSource(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read OUT-006 production source: " + path, exception);
        }
    }

    private static SubscriptionCreated createdEvent(PlanType planType, String correlationId, List<UUID> evaluationIds,
            BigDecimal amount) {
        return new SubscriptionCreated(EVENT_ID, OCCURRED_AT, SUBSCRIPTION_ID, PATIENT_ID,
                new ContractCode("NTC-2026-0001"), PLAN_ID, planType, LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 30), 30, evaluationIds, amount, correlationId);
    }

    private static ConsolidatedCalendarClosed calendarClosedEvent() {
        DeliveryAddress addressWithoutNumber = new DeliveryAddress(FIRST_ADDRESS_ID, "Casa", "Av. Cristo Redentor",
                null, "Santa Cruz", "Torre Norte", -17.78, -63.18, null);
        DeliveryAddress addressWithNumber = new DeliveryAddress(SECOND_ADDRESS_ID, "Oficina", "Calle 7", "123",
                "Cochabamba", "Edificio Sur", -17.39, -66.16, null);
        ConsolidatedDeliverySnapshot firstDelivery = new ConsolidatedDeliverySnapshot(FIRST_CALENDAR_DAY_ID,
                SUBSCRIPTION_ID, PATIENT_ID, "Ana Perez", PLAN_ID, 1, addressWithoutNumber, List.of(
                new ConsolidatedRecipeSnapshot(BREAKFAST_RECIPE_ID, "Desayuno", LocalTime.of(8, 5), 250),
                new ConsolidatedRecipeSnapshot(LUNCH_RECIPE_ID, "Almuerzo", LocalTime.of(13, 45), 350)));
        ConsolidatedDeliverySnapshot secondDelivery = new ConsolidatedDeliverySnapshot(SECOND_CALENDAR_DAY_ID,
                SECOND_SUBSCRIPTION_ID, SECOND_PATIENT_ID, "Beatriz Flores", SECOND_PLAN_ID, 2, addressWithNumber, List.of(
                new ConsolidatedRecipeSnapshot(DINNER_RECIPE_ID, "Cena", LocalTime.of(19, 30), 400),
                new ConsolidatedRecipeSnapshot(SNACK_RECIPE_ID, "Merienda", LocalTime.of(16, 15), 150)));
        return new ConsolidatedCalendarClosed(EVENT_ID, OCCURRED_AT, CALENDAR_ID, LocalDate.of(2026, 10, 29), 2,
                "flow-calendar", List.of(firstDelivery, secondDelivery));
    }

    private static UUID uuid(long value) {
        return new UUID(0, value);
    }

    private record UnsupportedEvent(UUID eventId, Instant occurredAt) implements DomainEvent { }
}
