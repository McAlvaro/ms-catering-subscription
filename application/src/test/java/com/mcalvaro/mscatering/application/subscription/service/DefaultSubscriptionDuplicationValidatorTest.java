package com.mcalvaro.mscatering.application.subscription.service;

import com.mcalvaro.mscatering.domain.core.DomainException;
import com.mcalvaro.mscatering.domain.patient.IPatientReferenceRepository;
import com.mcalvaro.mscatering.domain.patient.PatientReference;
import com.mcalvaro.mscatering.domain.subscription.ISubscriptionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultSubscriptionDuplicationValidatorTest {

    @Mock
    private ISubscriptionRepository subscriptionRepository;

    @Mock
    private IPatientReferenceRepository patientRepository;

    @InjectMocks
    private DefaultSubscriptionDuplicationValidator validator;

    @Test
    @DisplayName("Should throw DomainException SUB-014 when the patient does not exist")
    void shouldThrowDomainExceptionWhenPatientDoesNotExist() {
        // Arrange
        UUID patientId = UUID.randomUUID();
        when(patientRepository.findById(patientId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> validator.validate(patientId))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("code", "SUB-014");

        verify(patientRepository, times(1)).findById(patientId);
        verify(subscriptionRepository, never()).hasActiveOrPausedSubscription(any());
    }

    @Test
    @DisplayName("Should throw DomainException SUB-015 when the patient is inactive")
    void shouldThrowDomainExceptionWhenPatientIsInactive() {
        // Arrange
        UUID patientId = UUID.randomUUID();
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(buildPatient(patientId, false)));

        // Act & Assert
        assertThatThrownBy(() -> validator.validate(patientId))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("code", "SUB-015");

        verify(patientRepository, times(1)).findById(patientId);
        verify(subscriptionRepository, never()).hasActiveOrPausedSubscription(any());
    }

    @Test
    @DisplayName("Should throw DomainException SUB-013 when the patient has an active or paused subscription")
    void shouldThrowDomainExceptionWhenPatientHasAnActiveOrPausedSubscription() {
        // Arrange
        UUID patientId = UUID.randomUUID();
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(buildPatient(patientId, true)));
        when(subscriptionRepository.hasActiveOrPausedSubscription(patientId)).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> validator.validate(patientId))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("code", "SUB-013");

        verify(patientRepository, times(1)).findById(patientId);
        verify(subscriptionRepository, times(1)).hasActiveOrPausedSubscription(patientId);
    }

    @Test
    @DisplayName("Should allow validation when the patient is active and has no duplicate subscription")
    void shouldAllowValidationWhenPatientIsActiveAndHasNoDuplicateSubscription() {
        // Arrange
        UUID patientId = UUID.randomUUID();
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(buildPatient(patientId, true)));
        when(subscriptionRepository.hasActiveOrPausedSubscription(patientId)).thenReturn(false);

        // Act
        validator.validate(patientId);

        // Assert
        verify(patientRepository, times(1)).findById(patientId);
        verify(subscriptionRepository, times(1)).hasActiveOrPausedSubscription(patientId);
    }

    private PatientReference buildPatient(UUID patientId, boolean active) {
        return new PatientReference(patientId, active, Instant.now());
    }
}
