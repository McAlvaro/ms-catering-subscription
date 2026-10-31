package com.mcalvaro.mscatering.infrastructure.integration.inbound.kafka;

import com.mcalvaro.mscatering.infrastructure.integration.inbound.bc1.address.PatientAddressRegisteredEvent;
import com.mcalvaro.mscatering.infrastructure.integration.inbound.bc1.address.PatientAddressRegistrationContractException;
import com.mcalvaro.mscatering.infrastructure.integration.inbound.bc1.address.PatientAddressRegistrationInboundAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class PatientAddressRegistrationKafkaListenerTest {

    @Mock
    private PatientAddressRegistrationInboundAdapter inboundAdapter;

    private PatientAddressRegistrationKafkaListener listener;

    @BeforeEach
    void setUp() {
        listener = new PatientAddressRegistrationKafkaListener(inboundAdapter);
    }

    @Test
    void consumesValidPatientAddressRegisteredJsonAndDelegatesToInboundAdapter() {
        String validJson = """
                {
                  "eventId": "550e8400-e29b-41d4-a716-446655440000",
                  "eventName": "bc1.DireccionClienteRegistrada",
                  "eventVersion": "1.0",
                  "occurredOn": "2026-10-27T12:00:00Z",
                  "source": "MsPatientsConsulCore",
                  "correlationId": "corr-123",
                  "data": {
                    "idPaciente": "660e8400-e29b-41d4-a716-446655440001",
                    "idDireccion": "770e8400-e29b-41d4-a716-446655440002",
                    "alias": "Casa",
                    "direccion": "Av. Principal 123",
                    "referencia": "Frente al parque",
                    "ciudad": "Cochabamba",
                    "geolocalizacion": {
                      "latitud": -17.3935,
                      "longitud": -66.1570
                    }
                  }
                }
                """;

        assertThatCode(() -> listener.onMessage(validJson)).doesNotThrowAnyException();

        ArgumentCaptor<PatientAddressRegisteredEvent> eventCaptor =
                ArgumentCaptor.forClass(PatientAddressRegisteredEvent.class);
        verify(inboundAdapter).receive(eventCaptor.capture());

        PatientAddressRegisteredEvent event = eventCaptor.getValue();
        assertThat(event.eventId()).isEqualTo(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
        assertThat(event.eventName()).isEqualTo("bc1.DireccionClienteRegistrada");
        assertThat(event.eventVersion()).isEqualTo("1.0");
        assertThat(event.occurredOn()).isEqualTo(Instant.parse("2026-10-27T12:00:00Z"));
        assertThat(event.source()).isEqualTo("MsPatientsConsulCore");
        assertThat(event.correlationId()).isEqualTo("corr-123");
        assertThat(event.data()).isNotNull();
        assertThat(event.data().patientId()).isEqualTo("660e8400-e29b-41d4-a716-446655440001");
        assertThat(event.data().addressId()).isEqualTo("770e8400-e29b-41d4-a716-446655440002");
        assertThat(event.data().alias()).isEqualTo("Casa");
        assertThat(event.data().street()).isEqualTo("Av. Principal 123");
        assertThat(event.data().reference()).isEqualTo("Frente al parque");
        assertThat(event.data().city()).isEqualTo("Cochabamba");
        assertThat(event.data().geolocation()).isNotNull();
        assertThat(event.data().geolocation().latitude()).isEqualTo(-17.3935);
        assertThat(event.data().geolocation().longitude()).isEqualTo(-66.1570);
    }

    @Test
    void ignoresEventWhenEventNameDiffersFromSupportedEvent() {
        String ignoredEventJson = """
                {
                  "eventId": "550e8400-e29b-41d4-a716-446655440000",
                  "eventName": "bc1.PacienteRegistrado",
                  "eventVersion": "1.0",
                  "occurredOn": "2026-10-27T12:00:00Z",
                  "source": "MsPatientsConsulCore",
                  "correlationId": "corr-123",
                  "data": {
                    "idPaciente": "660e8400-e29b-41d4-a716-446655440001",
                    "nombreCompleto": "María Pérez"
                  }
                }
                """;

        assertThatCode(() -> listener.onMessage(ignoredEventJson)).doesNotThrowAnyException();

        verifyNoInteractions(inboundAdapter);
    }

    @Test
    void handlesMalformedJsonPoisonPillDefensivelyWithoutPropagatingException() {
        String poisonPill = "{ invalid-json: true, ";

        assertThatCode(() -> listener.onMessage(poisonPill)).doesNotThrowAnyException();

        verifyNoInteractions(inboundAdapter);
    }

    @Test
    void catchesPatientAddressRegistrationContractExceptionDefensivelyWithoutPropagating() {
        String validJson = """
                {
                  "eventId": "550e8400-e29b-41d4-a716-446655440000",
                  "eventName": "bc1.DireccionClienteRegistrada",
                  "eventVersion": "1.0",
                  "occurredOn": "2026-10-27T12:00:00Z",
                  "source": "MsPatientsConsulCore",
                  "correlationId": "corr-123",
                  "data": {
                    "idPaciente": "660e8400-e29b-41d4-a716-446655440001",
                    "idDireccion": "770e8400-e29b-41d4-a716-446655440002",
                    "alias": "Casa",
                    "direccion": "Av. Principal 123",
                    "referencia": "Frente al parque",
                    "ciudad": "Cochabamba",
                    "geolocalizacion": {
                      "latitud": -17.3935,
                      "longitud": -66.1570
                    }
                  }
                }
                """;

        doThrow(new PatientAddressRegistrationContractException("PROJ-001", "Contract violation"))
                .when(inboundAdapter).receive(any());

        assertThatCode(() -> listener.onMessage(validJson)).doesNotThrowAnyException();

        verify(inboundAdapter).receive(any(PatientAddressRegisteredEvent.class));
    }

    @Test
    void ignoresNullOrBlankPayloadDefensively() {
        assertThatCode(() -> listener.onMessage(null)).doesNotThrowAnyException();
        assertThatCode(() -> listener.onMessage("   ")).doesNotThrowAnyException();

        verifyNoInteractions(inboundAdapter);
    }
}
