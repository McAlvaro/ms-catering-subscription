package com.mcalvaro.mscatering.application.patient.synchronization;

import com.mcalvaro.mscatering.domain.core.id.PatientId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class SynchronizePatientReferenceCommandHandlerTest {

    @Mock
    private PatientReferenceSynchronizationStore synchronizationStore;

    @InjectMocks
    private SynchronizePatientReferenceCommandHandler handler;

    @Test
    void persistsOnlyTheMinimalSynchronizationCommand() {
        SynchronizePatientReferenceCommand command = new SynchronizePatientReferenceCommand(
                UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
                "bc1.PacienteRegistrado",
                "1.0",
                Instant.parse("2026-10-27T12:00:00Z"),
                "MsPatientsConsulCore",
                "corr-123",
                PatientId.of("PAC-660e8400-e29b-41d4-a716-446655440001"),
                "Ada Lovelace",
                "a".repeat(64));

        Void result = handler.handle(command);

        assertThat(result).isNull();
        assertThat(SynchronizePatientReferenceCommand.class.getRecordComponents())
                .extracting(component -> component.getName())
                .containsExactly("eventId", "eventName", "eventVersion", "occurredOn", "source", "correlationId",
                         "patientId", "patientName", "payloadFingerprint");
        verify(synchronizationStore).synchronize(command);
        verifyNoMoreInteractions(synchronizationStore);
    }
}
