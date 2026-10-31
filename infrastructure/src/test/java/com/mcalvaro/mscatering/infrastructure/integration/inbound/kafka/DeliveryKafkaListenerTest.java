package com.mcalvaro.mscatering.infrastructure.integration.inbound.kafka;

import com.mcalvaro.mscatering.domain.core.DomainException;
import com.mcalvaro.mscatering.infrastructure.integration.inbound.bc5.delivery.DeliveryConfirmedData;
import com.mcalvaro.mscatering.infrastructure.integration.inbound.bc5.delivery.DeliveryConfirmedEvent;
import com.mcalvaro.mscatering.infrastructure.integration.inbound.bc5.delivery.DeliveryContractException;
import com.mcalvaro.mscatering.infrastructure.integration.inbound.bc5.delivery.DeliveryFailedData;
import com.mcalvaro.mscatering.infrastructure.integration.inbound.bc5.delivery.DeliveryFailedEvent;
import com.mcalvaro.mscatering.infrastructure.integration.inbound.bc5.delivery.DeliveryInboundAdapter;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class DeliveryKafkaListenerTest {

    private static final String CONFIRMED_DELIVERY_JSON = """
            {
              "eventId": "11111111-1111-1111-1111-111111111111",
              "eventName": "bc5.EntregaConfirmada",
              "eventVersion": "1.0",
              "occurredOn": "2026-10-30T14:30:00Z",
              "source": "MsLogisticDelivery",
              "correlationId": "corr-bc5-001",
              "data": {
                "idEntrega": "22222222-2222-2222-2222-222222222222",
                "idPaquete": "33333333-3333-3333-3333-333333333333",
                "idCalendarioDia": "44444444-4444-4444-4444-444444444444",
                "idSuscripcion": "55555555-5555-5555-5555-555555555555",
                "idPaciente": "66666666-6666-6666-6666-666666666666",
                "fechaHoraEntrega": "2026-10-30T14:28:10Z",
                "recibidoPor": "Carlos Gomez",
                "evidenciaFotoUrl": "https://storage.nurti.com/evidencias/foto1.jpg",
                "geolocalizacion": {
                  "latitud": -17.3935,
                  "longitud": -66.1570
                }
              }
            }
            """;

    private static final String FAILED_DELIVERY_JSON = """
            {
              "eventId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
              "eventName": "bc5.EntregaNoConcretada",
              "eventVersion": "1.0",
              "occurredOn": "2026-10-30T15:00:00Z",
              "source": "MsLogisticDelivery",
              "correlationId": "corr-bc5-002",
              "data": {
                "idEntrega": "aaaaaaaa-1111-1111-1111-111111111111",
                "idPaquete": "bbbbbbbb-2222-2222-2222-222222222222",
                "idCalendarioDia": "cccccccc-3333-3333-3333-333333333333",
                "idSuscripcion": "55555555-5555-5555-5555-555555555555",
                "idPaciente": "66666666-6666-6666-6666-666666666666",
                "fechaEntrega": "2026-10-30",
                "intentosRealizados": 2,
                "motivoFinal": "Paciente no se encontraba en el domicilio",
                "aplicaCompensacion": true,
                "tipoCompensacion": "REPROGRAMACION"
              }
            }
            """;

    @Mock
    private DeliveryInboundAdapter inboundAdapter;

    private DeliveryKafkaListener listener;

    @BeforeEach
    void setUp() {
        listener = new DeliveryKafkaListener(inboundAdapter);
    }

    @Test
    @DisplayName("Consumes valid EntregaConfirmada JSON and delegates to receiveConfirmed")
    void consumesValidConfirmedDeliveryJsonAndDelegatesToReceiveConfirmed() {
        assertThatCode(() -> listener.onMessage(CONFIRMED_DELIVERY_JSON)).doesNotThrowAnyException();

        ArgumentCaptor<DeliveryConfirmedEvent> captor = ArgumentCaptor.forClass(DeliveryConfirmedEvent.class);
        verify(inboundAdapter).receiveConfirmed(captor.capture());
        verify(inboundAdapter, never()).receiveFailed(any());

        DeliveryConfirmedEvent event = captor.getValue();
        assertThat(event.eventId()).isEqualTo(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        assertThat(event.eventName()).isEqualTo("bc5.EntregaConfirmada");
        assertThat(event.eventVersion()).isEqualTo("1.0");
        assertThat(event.occurredOn()).isEqualTo(Instant.parse("2026-10-30T14:30:00Z"));
        assertThat(event.source()).isEqualTo("MsLogisticDelivery");
        assertThat(event.correlationId()).isEqualTo("corr-bc5-001");

        DeliveryConfirmedData data = event.data();
        assertThat(data).isNotNull();
        assertThat(data.idEntrega()).isEqualTo("22222222-2222-2222-2222-222222222222");
        assertThat(data.idPaquete()).isEqualTo("33333333-3333-3333-3333-333333333333");
        assertThat(data.idCalendarioDia()).isEqualTo("44444444-4444-4444-4444-444444444444");
        assertThat(data.idSuscripcion()).isEqualTo("55555555-5555-5555-5555-555555555555");
        assertThat(data.idPaciente()).isEqualTo("66666666-6666-6666-6666-666666666666");
        assertThat(data.fechaHoraEntrega()).isEqualTo("2026-10-30T14:28:10Z");
        assertThat(data.recibidoPor()).isEqualTo("Carlos Gomez");
        assertThat(data.evidenciaFotoUrl()).isEqualTo("https://storage.nurti.com/evidencias/foto1.jpg");
        assertThat(data.geolocalizacion()).isNotNull();
        assertThat(data.geolocalizacion().latitude()).isEqualTo(-17.3935);
        assertThat(data.geolocalizacion().longitude()).isEqualTo(-66.1570);
    }

    @Test
    @DisplayName("Consumes valid EntregaNoConcretada JSON and delegates to receiveFailed")
    void consumesValidFailedDeliveryJsonAndDelegatesToReceiveFailed() {
        assertThatCode(() -> listener.onMessage(FAILED_DELIVERY_JSON)).doesNotThrowAnyException();

        ArgumentCaptor<DeliveryFailedEvent> captor = ArgumentCaptor.forClass(DeliveryFailedEvent.class);
        verify(inboundAdapter).receiveFailed(captor.capture());
        verify(inboundAdapter, never()).receiveConfirmed(any());

        DeliveryFailedEvent event = captor.getValue();
        assertThat(event.eventId()).isEqualTo(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
        assertThat(event.eventName()).isEqualTo("bc5.EntregaNoConcretada");
        assertThat(event.eventVersion()).isEqualTo("1.0");
        assertThat(event.occurredOn()).isEqualTo(Instant.parse("2026-10-30T15:00:00Z"));
        assertThat(event.source()).isEqualTo("MsLogisticDelivery");
        assertThat(event.correlationId()).isEqualTo("corr-bc5-002");

        DeliveryFailedData data = event.data();
        assertThat(data).isNotNull();
        assertThat(data.idEntrega()).isEqualTo("aaaaaaaa-1111-1111-1111-111111111111");
        assertThat(data.idPaquete()).isEqualTo("bbbbbbbb-2222-2222-2222-222222222222");
        assertThat(data.idCalendarioDia()).isEqualTo("cccccccc-3333-3333-3333-333333333333");
        assertThat(data.idSuscripcion()).isEqualTo("55555555-5555-5555-5555-555555555555");
        assertThat(data.idPaciente()).isEqualTo("66666666-6666-6666-6666-666666666666");
        assertThat(data.fechaEntrega()).isEqualTo(LocalDate.of(2026, 10, 30));
        assertThat(data.intentosRealizados()).isEqualTo(2);
        assertThat(data.motivoFinal()).isEqualTo("Paciente no se encontraba en el domicilio");
        assertThat(data.aplicaCompensacion()).isTrue();
        assertThat(data.tipoCompensacion()).isEqualTo("REPROGRAMACION");
    }

    @Test
    @DisplayName("Ignores event with unknown or unsupported eventName")
    void ignoresEventWhenEventNameIsUnknown() {
        String unknownEventJson = """
                {
                  "eventId": "11111111-1111-1111-1111-111111111111",
                  "eventName": "bc5.RutaAsignada",
                  "eventVersion": "1.0",
                  "occurredOn": "2026-10-30T14:30:00Z",
                  "source": "MsLogisticDelivery",
                  "correlationId": "corr-bc5-003",
                  "data": {}
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
    @DisplayName("Catches DeliveryContractException thrown by adapter receiveConfirmed defensively")
    void catchesDeliveryContractExceptionFromReceiveConfirmedDefensively() {
        doThrow(new DeliveryContractException("DEL-001", "Delivery day not found"))
                .when(inboundAdapter).receiveConfirmed(any());

        assertThatCode(() -> listener.onMessage(CONFIRMED_DELIVERY_JSON)).doesNotThrowAnyException();
        verify(inboundAdapter).receiveConfirmed(any());
    }

    @Test
    @DisplayName("Catches DeliveryContractException thrown by adapter receiveFailed defensively")
    void catchesDeliveryContractExceptionFromReceiveFailedDefensively() {
        doThrow(new DeliveryContractException("DEL-001", "Delivery day not found"))
                .when(inboundAdapter).receiveFailed(any());

        assertThatCode(() -> listener.onMessage(FAILED_DELIVERY_JSON)).doesNotThrowAnyException();
        verify(inboundAdapter).receiveFailed(any());
    }

    @Test
    @DisplayName("Catches generic DomainException thrown by adapter defensively")
    void catchesDomainExceptionDefensively() {
        doThrow(new DomainException("PROJ-001", "Contract violation"))
                .when(inboundAdapter).receiveConfirmed(any());

        assertThatCode(() -> listener.onMessage(CONFIRMED_DELIVERY_JSON)).doesNotThrowAnyException();
        verify(inboundAdapter).receiveConfirmed(any());
    }

    @Test
    @DisplayName("Catches unexpected Exception thrown by adapter defensively")
    void catchesUnexpectedExceptionDefensively() {
        doThrow(new RuntimeException("Simulated unexpected database failure"))
                .when(inboundAdapter).receiveConfirmed(any());

        assertThatCode(() -> listener.onMessage(CONFIRMED_DELIVERY_JSON)).doesNotThrowAnyException();
        verify(inboundAdapter).receiveConfirmed(any());
    }

    @Test
    @DisplayName("Validates constructor parameters fail on null")
    void constructorRejectsNullDependencies() {
        assertThrows(NullPointerException.class, () -> new DeliveryKafkaListener(null, DeliveryKafkaListener.defaultObjectMapper()));
        assertThrows(NullPointerException.class, () -> new DeliveryKafkaListener(inboundAdapter, null));
        assertThrows(NullPointerException.class, () -> new DeliveryKafkaListener(null));
    }

    @Test
    @DisplayName("Works with custom ObjectMapper and ignores unknown properties safely")
    void worksWithCustomObjectMapperAndIgnoresUnknownProperties() {
        DeliveryKafkaListener customListener = new DeliveryKafkaListener(
                inboundAdapter,
                DeliveryKafkaListener.defaultObjectMapper()
        );

        String jsonWithUnknownFields = """
                {
                  "eventId": "11111111-1111-1111-1111-111111111111",
                  "eventName": "bc5.EntregaConfirmada",
                  "eventVersion": "1.0",
                  "occurredOn": "2026-10-30T14:30:00Z",
                  "source": "MsLogisticDelivery",
                  "correlationId": "corr-bc5-001",
                  "unexpectedRootField": "foo",
                  "data": {
                    "idEntrega": "22222222-2222-2222-2222-222222222222",
                    "idPaquete": "33333333-3333-3333-3333-333333333333",
                    "idCalendarioDia": "44444444-4444-4444-4444-444444444444",
                    "idSuscripcion": "55555555-5555-5555-5555-555555555555",
                    "idPaciente": "66666666-6666-6666-6666-666666666666",
                    "fechaHoraEntrega": "2026-10-30T14:28:10Z",
                    "extraDataField": "bar"
                  }
                }
                """;

        assertThatCode(() -> customListener.onMessage(jsonWithUnknownFields)).doesNotThrowAnyException();
        verify(inboundAdapter).receiveConfirmed(any());
    }

    @Test
    @DisplayName("Successfully deserializes message when identifiers are UUID string formatted")
    void successfullyDeserializesWhenIdentifiersAreUuidStrings() {
        String uuidJson = """
                {
                  "eventId": "11111111-1111-1111-1111-111111111111",
                  "eventName": "bc5.EntregaConfirmada",
                  "eventVersion": "1.0",
                  "occurredOn": "2026-10-30T14:30:00Z",
                  "source": "MsLogisticDelivery",
                  "correlationId": "corr-bc5-uuid",
                  "data": {
                    "idEntrega": "22222222-2222-2222-2222-222222222222",
                    "idPaquete": "33333333-3333-3333-3333-333333333333",
                    "idCalendarioDia": "44444444-4444-4444-4444-444444444444",
                    "idSuscripcion": "55555555-5555-5555-5555-555555555555",
                    "idPaciente": "66666666-6666-6666-6666-666666666666",
                    "fechaHoraEntrega": "2026-10-30T14:28:10Z",
                    "recibidoPor": "Carlos Gomez"
                  }
                }
                """;

        assertThatCode(() -> listener.onMessage(uuidJson)).doesNotThrowAnyException();

        ArgumentCaptor<DeliveryConfirmedEvent> captor = ArgumentCaptor.forClass(DeliveryConfirmedEvent.class);
        verify(inboundAdapter).receiveConfirmed(captor.capture());
        DeliveryConfirmedData data = captor.getValue().data();
        assertThat(data.idEntrega()).isEqualTo("22222222-2222-2222-2222-222222222222");
        assertThat(data.idPaquete()).isEqualTo("33333333-3333-3333-3333-333333333333");
        assertThat(data.idCalendarioDia()).isEqualTo("44444444-4444-4444-4444-444444444444");
    }
}
