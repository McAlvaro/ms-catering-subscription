package com.mcalvaro.mscatering.application.patient;

import com.mcalvaro.mscatering.domain.patient.IPatientReferenceRepository;
import com.mcalvaro.mscatering.domain.patient.PatientReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SavePatientReferenceCommandHandlerTest {

    @Mock
    private IPatientReferenceRepository patientRepository;

    @InjectMocks
    private SavePatientReferenceCommandHandler handler;

    @Test
    @DisplayName("Should save the patient reference with the provided timestamp and return null")
    void shouldSavePatientReferenceWithProvidedTimestampAndReturnNull() {
        // Arrange
        UUID patientId = UUID.randomUUID();
        Instant updatedAt = Instant.parse("2026-08-31T10:15:30Z");
        SavePatientReferenceCommand command = new SavePatientReferenceCommand(patientId, true, updatedAt);
        ArgumentCaptor<PatientReference> patientReferenceCaptor = ArgumentCaptor.forClass(PatientReference.class);

        // Act
        Void result = handler.handle(command);

        // Assert
        assertThat(result).isNull();
        verify(patientRepository, times(1)).save(patientReferenceCaptor.capture());
        assertThat(patientReferenceCaptor.getValue())
                .isEqualTo(new PatientReference(patientId, true, updatedAt));
    }

    @Test
    @DisplayName("Should save the patient reference with the current timestamp when no timestamp is provided")
    void shouldSavePatientReferenceWithCurrentTimestampWhenTimestampIsNull() {
        // Arrange
        UUID patientId = UUID.randomUUID();
        SavePatientReferenceCommand command = new SavePatientReferenceCommand(patientId, false, null);
        ArgumentCaptor<PatientReference> patientReferenceCaptor = ArgumentCaptor.forClass(PatientReference.class);

        // Act
        Instant beforeHandling = Instant.now();
        Void result = handler.handle(command);
        Instant afterHandling = Instant.now();

        // Assert
        assertThat(result).isNull();
        verify(patientRepository, times(1)).save(patientReferenceCaptor.capture());
        PatientReference savedPatientReference = patientReferenceCaptor.getValue();
        assertThat(savedPatientReference.patientId()).isEqualTo(patientId);
        assertThat(savedPatientReference.active()).isFalse();
        assertThat(savedPatientReference.updatedAt())
                .isBetween(beforeHandling, afterHandling);
    }
}
