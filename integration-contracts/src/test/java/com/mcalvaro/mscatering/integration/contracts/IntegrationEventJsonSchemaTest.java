package com.mcalvaro.mscatering.integration.contracts;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.networknt.schema.InputFormat;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class IntegrationEventJsonSchemaTest {
    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();
    private static final String SCHEMA_PREFIX = "https://ms-catering-subscription.example/schemas/outbox/v1";

    @ParameterizedTest
    @MethodSource("events")
    void serializesEachContractToItsFrozenV1Schema(IntegrationEvent event) throws Exception {
        SchemaRegistry registry = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12,
                builder -> builder.schemaIdResolvers(resolvers -> resolvers.mapPrefix(SCHEMA_PREFIX, "classpath:schemas/v1")));
        String schemaName = switch (event.eventName()) {
            case "bc3.SuscripcionCreada" -> "suscripcion-creada.schema.json";
            case "bc3.SuscripcionPausada" -> "suscripcion-pausada.schema.json";
            case "bc3.SuscripcionReactivada" -> "suscripcion-reactivada.schema.json";
            case "bc3.SuscripcionCancelada" -> "suscripcion-cancelada.schema.json";
            case "bc3.CalendarioConsolidadoCerrado" -> "calendario-consolidado-cerrado.schema.json";
            default -> throw new IllegalArgumentException("Unexpected contract event");
        };
        Schema schema = registry.getSchema(SchemaLocation.of(SCHEMA_PREFIX + "/" + schemaName));
        schema.initializeValidators();

        String json = MAPPER.writeValueAsString(event);

        assertThat(schema.validate(json, InputFormat.JSON,
                context -> context.executionConfig(config -> config.formatAssertionsEnabled(true))))
                .as(json)
                .isEmpty();
        assertThat(json).doesNotContain("logicalDestination");
        if (event instanceof SubscriptionCreatedIntegrationEvent) {
            assertThat(MAPPER.readTree(json).at("/data/nroFactura").isNull()).isTrue();
        }
    }

    private static Stream<IntegrationEvent> events() {
        UUID eventId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        String subscriptionId = "SUS-00000000-0000-0000-0000-000000000002";
        String patientId = "PAC-00000000-0000-0000-0000-000000000003";
        String planId = "PLAN-00000000-0000-0000-0000-000000000004";
        Instant instant = Instant.parse("2026-10-29T12:00:00Z");
        return Stream.of(
                new SubscriptionCreatedIntegrationEvent(eventId, instant, "correlation",
                        new SubscriptionCreatedIntegrationEvent.Data(subscriptionId, patientId, planId, "PERSONALIZADO",
                                30, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 30), 2,
                                new BigDecimal("250.50"), null)),
                new SubscriptionPausedIntegrationEvent(eventId, instant, "correlation",
                        new SubscriptionPausedIntegrationEvent.Data(subscriptionId, patientId, LocalDate.of(2026, 10, 5),
                                LocalDate.of(2026, 10, 8), "Viaje", LocalDate.of(2026, 11, 2), LocalDate.of(2026, 10, 1))),
                new SubscriptionReactivatedIntegrationEvent(eventId, instant, "correlation",
                        new SubscriptionReactivatedIntegrationEvent.Data(subscriptionId, patientId, LocalDate.of(2026, 10, 8),
                                LocalDate.of(2026, 11, 2))),
                new SubscriptionCancelledIntegrationEvent(eventId, instant, "correlation",
                        new SubscriptionCancelledIntegrationEvent.Data(subscriptionId, patientId, planId, "MANUAL", "Solicitud", LocalDate.of(2026, 10, 8), 1)),
                new ConsolidatedCalendarClosedIntegrationEvent(eventId, instant, "correlation",
                        new ConsolidatedCalendarClosedIntegrationEvent.Data("CC-00000000-0000-0000-0000-000000000005",
                                LocalDate.of(2026, 10, 29), instant, 1,
                                List.of(new ConsolidatedCalendarClosedIntegrationEvent.Delivery("DIA-00000000-0000-0000-0000-000000000006",
                                        subscriptionId, patientId, "Ana", planId, 1,
                                        new ConsolidatedCalendarClosedIntegrationEvent.Address("DIR-00000000-0000-0000-0000-000000000007", "Casa", "Calle 1", "Referencia", "Santa Cruz", -17.78, -63.18),
                                        List.of(new ConsolidatedCalendarClosedIntegrationEvent.Recipe("REC-00000000-0000-0000-0000-000000000008", "Sopa", "08:30", new BigDecimal("250"))))))));
    }
}
