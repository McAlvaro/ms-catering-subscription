package com.mcalvaro.mscatering.infrastructure.integration.inbound.bc1.patient;

import an.awesome.pipelinr.Pipeline;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcalvaro.mscatering.application.patient.synchronization.SynchronizePatientReferenceCommand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.RecordComponent;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class PatientRegistrationInboundAdapterTest {

    @Mock
    private Pipeline pipeline;

    @InjectMocks
    private PatientRegistrationInboundAdapter adapter;

    @Test
    void consumesOperationalPatientNameAndDispatchesTheLocalCommand() throws Exception {
        PatientRegisteredEvent event = eventWithIgnoredPii();
        ArgumentCaptor<SynchronizePatientReferenceCommand> commandCaptor =
                ArgumentCaptor.forClass(SynchronizePatientReferenceCommand.class);

        adapter.receive(event);

        verify(pipeline).send(commandCaptor.capture());
        SynchronizePatientReferenceCommand command = commandCaptor.getValue();
        assertThat(command.eventId()).isEqualTo(event.eventId());
        assertThat(command.eventName()).isEqualTo("bc1.PacienteRegistrado");
        assertThat(command.eventVersion()).isEqualTo("1.0");
        assertThat(command.occurredOn()).isEqualTo(event.occurredOn());
        assertThat(command.source()).isEqualTo("MsPatientsConsulCore");
        assertThat(command.correlationId()).isEqualTo(event.correlationId());
        assertThat(command.patientId().value()).isEqualTo(event.data().patientId());
        assertThat(command.patientName()).isEqualTo("María Pérez");
        assertThat(command.payloadFingerprint()).isNotBlank();
        assertThat(command.getClass().getRecordComponents())
                .extracting(RecordComponent::getName)
                .containsExactly("eventId", "eventName", "eventVersion", "occurredOn", "source", "correlationId",
                         "patientId", "patientName", "payloadFingerprint");
    }

    @Test
    void rejectsInvalidEnvelopeOrConsumedDataBeforeDispatching() {
        PatientRegisteredEvent valid = validEvent();
        List<PatientRegisteredEvent> invalidEvents = List.of(
                new PatientRegisteredEvent(null, valid.eventName(), valid.eventVersion(), valid.occurredOn(),
                        valid.source(), valid.correlationId(), valid.data()),
                new PatientRegisteredEvent(valid.eventId(), "PacienteRegistrado", valid.eventVersion(),
                        valid.occurredOn(), valid.source(), valid.correlationId(), valid.data()),
                new PatientRegisteredEvent(valid.eventId(), valid.eventName(), "2.0", valid.occurredOn(),
                        valid.source(), valid.correlationId(), valid.data()),
                new PatientRegisteredEvent(valid.eventId(), valid.eventName(), valid.eventVersion(), null,
                        valid.source(), valid.correlationId(), valid.data()),
                new PatientRegisteredEvent(valid.eventId(), valid.eventName(), valid.eventVersion(), valid.occurredOn(),
                        "OtherSource", valid.correlationId(), valid.data()),
                new PatientRegisteredEvent(valid.eventId(), valid.eventName(), valid.eventVersion(), valid.occurredOn(),
                        valid.source(), "   ", valid.data()),
                new PatientRegisteredEvent(valid.eventId(), valid.eventName(), valid.eventVersion(), valid.occurredOn(),
                        valid.source(), valid.correlationId(), null),
                new PatientRegisteredEvent(valid.eventId(), valid.eventName(), valid.eventVersion(), valid.occurredOn(),
                         valid.source(), valid.correlationId(), new PatientRegisteredData(null, "Ada Lovelace")));

        invalidEvents.forEach(event -> assertThatThrownBy(() -> adapter.receive(event))
                .isInstanceOf(PatientRegistrationContractException.class)
                .extracting("code")
                .isEqualTo("PROJ-001"));
        verifyNoInteractions(pipeline);
    }

    private PatientRegisteredEvent validEvent() {
        return new PatientRegisteredEvent(
                UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
                "bc1.PacienteRegistrado",
                "1.0",
                Instant.parse("2026-10-27T12:00:00Z"),
                "MsPatientsConsulCore",
                "corr-123",
                new PatientRegisteredData("PAC-660e8400", "Ada Lovelace"));
    }

    private PatientRegisteredEvent eventWithIgnoredPii() throws Exception {
        return new ObjectMapper().findAndRegisterModules().readValue("""
                {
                  "eventId": "550e8400-e29b-41d4-a716-446655440000",
                  "eventName": "bc1.PacienteRegistrado",
                  "eventVersion": "1.0",
                  "occurredOn": "2026-10-27T12:00:00Z",
                  "source": "MsPatientsConsulCore",
                  "correlationId": "corr-123",
                  "data": {
                    "idPaciente": "PAC-660e8400",
                    "nombreCompleto": "María Pérez",
                    "telefono": "+51 999 111 222",
                    "email": "maria.perez@example.com"
                  }
                }
                """, PatientRegisteredEvent.class);
    }
}
