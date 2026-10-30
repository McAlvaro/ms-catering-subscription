package com.mcalvaro.mscatering.infrastructure.integration.inbound.kafka;

import com.mcalvaro.mscatering.infrastructure.integration.inbound.bc1.patient.PatientRegisteredEvent;
import com.mcalvaro.mscatering.infrastructure.integration.inbound.bc1.patient.PatientRegistrationContractException;
import com.mcalvaro.mscatering.infrastructure.integration.inbound.bc1.patient.PatientRegistrationInboundAdapter;
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
class PatientRegistrationKafkaListenerTest {

    @Mock
    private PatientRegistrationInboundAdapter inboundAdapter;

    private PatientRegistrationKafkaListener listener;

    @BeforeEach
    void setUp() {
        listener = new PatientRegistrationKafkaListener(inboundAdapter);
    }

    @Test
    void consumesValidPatientRegisteredJsonAndDelegatesToInboundAdapter() {
        String validJson = """
                {
                  "eventId": "550e8400-e29b-41d4-a716-446655440000",
                  "eventName": "bc1.PacienteRegistrado",
                  "eventVersion": "1.0",
                  "occurredOn": "2026-10-27T12:00:00Z",
                  "source": "MsPatientsConsulCore",
                  "correlationId": "corr-123",
                  "data": {
                    "idPaciente": "660e8400-e29b-41d4-a716-446655440001",
                    "nombreCompleto": "María Pérez",
                    "telefono": "+51 999 111 222",
                    "email": "maria.perez@example.com"
                  }
                }
                """;

        assertThatCode(() -> listener.onMessage(validJson)).doesNotThrowAnyException();

        ArgumentCaptor<PatientRegisteredEvent> eventCaptor = ArgumentCaptor.forClass(PatientRegisteredEvent.class);
        verify(inboundAdapter).receive(eventCaptor.capture());

        PatientRegisteredEvent event = eventCaptor.getValue();
        assertThat(event.eventId()).isEqualTo(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
        assertThat(event.eventName()).isEqualTo("bc1.PacienteRegistrado");
        assertThat(event.eventVersion()).isEqualTo("1.0");
        assertThat(event.occurredOn()).isEqualTo(Instant.parse("2026-10-27T12:00:00Z"));
        assertThat(event.source()).isEqualTo("MsPatientsConsulCore");
        assertThat(event.correlationId()).isEqualTo("corr-123");
        assertThat(event.data()).isNotNull();
        assertThat(event.data().patientId()).isEqualTo(UUID.fromString("660e8400-e29b-41d4-a716-446655440001"));
        assertThat(event.data().patientName()).isEqualTo("María Pérez");
    }

    @Test
    void ignoresEventWhenEventNameDiffersFromSupportedEvent() {
        String ignoredEventJson = """
                {
                  "eventId": "550e8400-e29b-41d4-a716-446655440000",
                  "eventName": "bc1.PacienteActualizado",
                  "eventVersion": "1.0",
                  "occurredOn": "2026-10-27T12:00:00Z",
                  "source": "MsPatientsConsulCore",
                  "correlationId": "corr-123",
                  "data": {
                    "idPaciente": "660e8400-e29b-41d4-a716-446655440001",
                    "nombreCompleto": "María Pérez Modificada"
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
    void catchesPatientRegistrationContractExceptionDefensivelyWithoutPropagating() {
        String validJson = """
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

        doThrow(new PatientRegistrationContractException("PROJ-001", "Contract violation"))
                .when(inboundAdapter).receive(any());

        assertThatCode(() -> listener.onMessage(validJson)).doesNotThrowAnyException();

        verify(inboundAdapter).receive(any(PatientRegisteredEvent.class));
    }

    @Test
    void ignoresNullOrBlankPayloadDefensively() {
        assertThatCode(() -> listener.onMessage(null)).doesNotThrowAnyException();
        assertThatCode(() -> listener.onMessage("   ")).doesNotThrowAnyException();

        verifyNoInteractions(inboundAdapter);
    }
}
