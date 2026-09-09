package com.mcalvaro.mscatering.application.subscription.CreateSubscription;

import com.mcalvaro.mscatering.domain.core.DomainException;
import com.mcalvaro.mscatering.domain.subscription.ISubscriptionRepository;
import com.mcalvaro.mscatering.domain.subscription.Subscription;
import com.mcalvaro.mscatering.domain.subscription.entity.BiweeklyEvaluation;
import com.mcalvaro.mscatering.domain.subscription.enums.EvaluationStatus;
import com.mcalvaro.mscatering.domain.subscription.enums.PlanDuration;
import com.mcalvaro.mscatering.domain.subscription.enums.ServiceType;
import com.mcalvaro.mscatering.domain.subscription.enums.SubscriptionStatus;
import com.mcalvaro.mscatering.domain.subscription.service.BiweeklyEvaluationGenerator;
import com.mcalvaro.mscatering.domain.subscription.service.SubscriptionDuplicationValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateSubscriptionCommandHandlerTest {

    @Mock
    private ISubscriptionRepository subscriptionRepository;

    @Mock
    private SubscriptionDuplicationValidator duplicationValidator;

    @Mock
    private BiweeklyEvaluationGenerator evaluationGenerator;

    @InjectMocks
    private CreateSubscriptionCommandHandler handler;

    @Test
    @DisplayName("Should create an active subscription with generated evaluations when the command is valid")
    void shouldCreateSubscriptionWithGeneratedEvaluationsWhenCommandIsValid() {
        // Arrange
        CreateSubscriptionCommand command = buildValidCommand();
        BiweeklyEvaluation evaluation = buildEvaluation(command.patientId());
        when(subscriptionRepository.getNextContractSequenceOfYear()).thenReturn(42);
        when(evaluationGenerator.generate(command.patientId(), command.startDate(), PlanDuration.BIWEEKLY))
                .thenReturn(List.of(evaluation));
        ArgumentCaptor<Subscription> subscriptionCaptor = ArgumentCaptor.forClass(Subscription.class);

        // Act
        UUID returnedId = handler.handle(command);

        // Assert
        verify(duplicationValidator, times(1)).validate(command.patientId());
        verify(subscriptionRepository, times(1)).getNextContractSequenceOfYear();
        verify(evaluationGenerator, times(1)).generate(
                command.patientId(), command.startDate(), PlanDuration.BIWEEKLY);
        verify(subscriptionRepository, times(1)).save(subscriptionCaptor.capture());

        Subscription savedSubscription = subscriptionCaptor.getValue();
        assertThat(returnedId).isEqualTo(savedSubscription.getId());
        assertThat(savedSubscription.getPatientId()).isEqualTo(command.patientId());
        assertThat(savedSubscription.getDietPlanId()).isEqualTo(command.dietPlanId());
        assertThat(savedSubscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(savedSubscription.getContract().period().startDate()).isEqualTo(command.startDate());
        assertThat(savedSubscription.getContract().period().endDate()).isEqualTo(command.endDate());
        assertThat(savedSubscription.getContract().serviceType()).isEqualTo(ServiceType.valueOf(command.serviceType()));
        assertThat(savedSubscription.getEvaluations()).containsExactly(evaluation);
        assertThat(savedSubscription.getEvaluations().get(0).getPatientId()).isEqualTo(command.patientId());
        assertThat(savedSubscription.getEvaluations().get(0).getStatus()).isEqualTo(EvaluationStatus.PENDING);
    }

    @Test
    @DisplayName("Should propagate DomainException and skip creation collaborators when duplication validation fails")
    void shouldPropagateDomainExceptionAndSkipCreationCollaboratorsWhenDuplicationValidationFails() {
        // Arrange
        CreateSubscriptionCommand command = buildValidCommand();
        DomainException exception = new DomainException("SUB-013", "Duplicate subscription.");
        org.mockito.Mockito.doThrow(exception).when(duplicationValidator).validate(command.patientId());

        // Act & Assert
        assertThatThrownBy(() -> handler.handle(command))
                .isSameAs(exception)
                .hasFieldOrPropertyWithValue("code", "SUB-013");

        verify(duplicationValidator, times(1)).validate(command.patientId());
        verify(subscriptionRepository, never()).getNextContractSequenceOfYear();
        verify(evaluationGenerator, never()).generate(any(), any(), any());
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw DomainException VO-008 and skip generation and persistence when dates are invalid")
    void shouldThrowDomainExceptionWhenValidityPeriodDatesAreInvalid() {
        // Arrange
        CreateSubscriptionCommand command = buildValidCommand(
                LocalDate.of(2026, 1, 15), LocalDate.of(2026, 1, 1), new BigDecimal("150.00"));

        // Act & Assert
        assertThatThrownBy(() -> handler.handle(command))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("code", "VO-008");

        verify(duplicationValidator, times(1)).validate(command.patientId());
        verify(subscriptionRepository, never()).getNextContractSequenceOfYear();
        verify(evaluationGenerator, never()).generate(any(), any(), any());
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw DomainException SUB-006 and skip generation and persistence when price is invalid")
    void shouldThrowDomainExceptionWhenContractPriceIsInvalid() {
        // Arrange
        CreateSubscriptionCommand command = buildValidCommand(
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 15), BigDecimal.ZERO);

        // Act & Assert
        assertThatThrownBy(() -> handler.handle(command))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("code", "SUB-006");

        verify(duplicationValidator, times(1)).validate(command.patientId());
        verify(subscriptionRepository, never()).getNextContractSequenceOfYear();
        verify(evaluationGenerator, never()).generate(any(), any(), any());
        verify(subscriptionRepository, never()).save(any());
    }

    private CreateSubscriptionCommand buildValidCommand() {
        return buildValidCommand(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 15), new BigDecimal("150.00"));
    }

    private CreateSubscriptionCommand buildValidCommand(LocalDate startDate, LocalDate endDate, BigDecimal totalPrice) {
        return new CreateSubscriptionCommand(
                UUID.randomUUID(),
                UUID.randomUUID(),
                startDate,
                endDate,
                ServiceType.FULL.name(),
                totalPrice,
                "Accepted terms and conditions",
                "Main Street",
                "123",
                "Cochabamba",
                "Near the park",
                -17.3935,
                -66.1570,
                "+59170000000",
                LocalTime.of(9, 0),
                LocalTime.of(11, 0),
                "Ring the doorbell");
    }

    private BiweeklyEvaluation buildEvaluation(UUID patientId) {
        return new BiweeklyEvaluation(UUID.randomUUID(), patientId, 1, LocalDate.of(2026, 1, 15));
    }
}
