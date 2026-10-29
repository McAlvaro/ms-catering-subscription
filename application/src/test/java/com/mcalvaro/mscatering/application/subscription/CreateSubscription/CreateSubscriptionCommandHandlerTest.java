package com.mcalvaro.mscatering.application.subscription.CreateSubscription;

import com.mcalvaro.mscatering.domain.core.DomainException;
import com.mcalvaro.mscatering.domain.subscription.ISubscriptionRepository;
import com.mcalvaro.mscatering.domain.subscription.Subscription;
import com.mcalvaro.mscatering.domain.subscription.entity.BiweeklyEvaluation;
import com.mcalvaro.mscatering.domain.subscription.event.SubscriptionCreated;
import com.mcalvaro.mscatering.domain.subscription.enums.EvaluationStatus;
import com.mcalvaro.mscatering.domain.subscription.enums.PlanDuration;
import com.mcalvaro.mscatering.domain.subscription.enums.ServiceType;
import com.mcalvaro.mscatering.domain.subscription.enums.SubscriptionStatus;
import com.mcalvaro.mscatering.domain.subscription.service.BiweeklyEvaluationGenerator;
import com.mcalvaro.mscatering.domain.subscription.service.SubscriptionDuplicationValidator;
import com.mcalvaro.mscatering.application.patient.address.DeliveryAddressResolver;
import com.mcalvaro.mscatering.application.dietplan.DietPlanResolver;
import com.mcalvaro.mscatering.domain.dietplan.DietPlanProjection;
import com.mcalvaro.mscatering.domain.dietplan.PlanType;
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
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateSubscriptionCommandHandlerTest {

    private static final UUID DELIVERY_ADDRESS_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440010");

    @Mock
    private ISubscriptionRepository subscriptionRepository;

    @Mock
    private SubscriptionDuplicationValidator duplicationValidator;

    @Mock
    private BiweeklyEvaluationGenerator evaluationGenerator;

    @Mock
    private DeliveryAddressResolver deliveryAddressResolver;

    @Mock
    private DietPlanResolver dietPlanResolver;

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
        when(deliveryAddressResolver.resolve(command.patientId(), command.deliveryAddressId()))
                .thenReturn(new com.mcalvaro.mscatering.domain.subscription.vo.DeliveryAddress("Main Street", null,
                        "Cochabamba", "Near park", -17.39, -66.15, null));
        when(dietPlanResolver.resolve(command.dietPlanId(), command.patientId(), command.startDate(), command.endDate()))
                .thenReturn(new DietPlanProjection(command.dietPlanId(), PlanType.BASE, null, true, command.startDate(), 15,
                        IntStream.rangeClosed(1, 15).mapToObj(day -> new com.mcalvaro.mscatering.domain.dietplan.DietPlanDay(UUID.randomUUID(), day, 1,
                                List.of(new com.mcalvaro.mscatering.domain.dietplan.DietPlanMeal(UUID.randomUUID(), com.mcalvaro.mscatering.domain.dietplan.MealType.LUNCH, LocalTime.NOON,
                                        List.of(new com.mcalvaro.mscatering.domain.dietplan.DietPlanRecipe(UUID.randomUUID(), "Meal", 1)))))).toList()));
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
    @DisplayName("Should snapshot the personalized plan type without falling back to BASE")
    void shouldCreatePersonalizedSubscriptionWithPersonalizedContractAndEventSnapshot() {
        CreateSubscriptionCommand command = buildValidCommand();
        BiweeklyEvaluation evaluation = buildEvaluation(command.patientId());
        when(subscriptionRepository.getNextContractSequenceOfYear()).thenReturn(42);
        when(evaluationGenerator.generate(command.patientId(), command.startDate(), PlanDuration.BIWEEKLY))
                .thenReturn(List.of(evaluation));
        when(deliveryAddressResolver.resolve(command.patientId(), command.deliveryAddressId()))
                .thenReturn(new com.mcalvaro.mscatering.domain.subscription.vo.DeliveryAddress("Main Street", null,
                        "Cochabamba", "Near park", -17.39, -66.15, null));
        when(dietPlanResolver.resolve(command.dietPlanId(), command.patientId(), command.startDate(), command.endDate()))
                .thenReturn(plan(command, PlanType.PERSONALIZED, command.patientId()));
        ArgumentCaptor<Subscription> subscriptionCaptor = ArgumentCaptor.forClass(Subscription.class);

        handler.handle(command);

        verify(subscriptionRepository).save(subscriptionCaptor.capture());
        Subscription subscription = subscriptionCaptor.getValue();
        assertThat(subscription.getContract().planType()).isEqualTo(PlanType.PERSONALIZED);
        assertThat(subscription.getDomainEvents()).singleElement()
                .isInstanceOfSatisfying(SubscriptionCreated.class,
                        event -> assertThat(event.planType()).isEqualTo(PlanType.PERSONALIZED));
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
        when(dietPlanResolver.resolve(command.dietPlanId(), command.patientId(), command.startDate(), command.endDate()))
                .thenReturn(plan(command, PlanType.BASE, null));

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
                DELIVERY_ADDRESS_ID,
                UUID.randomUUID(),
                startDate,
                endDate,
                ServiceType.FULL.name(),
                totalPrice,
                "Accepted terms and conditions",
                UUID.randomUUID(),
                LocalTime.of(9, 0),
                LocalTime.of(11, 0),
                "Ring the doorbell");
    }

    private BiweeklyEvaluation buildEvaluation(UUID patientId) {
        return new BiweeklyEvaluation(UUID.randomUUID(), patientId, 1, LocalDate.of(2026, 1, 15));
    }

    private DietPlanProjection plan(CreateSubscriptionCommand command, PlanType type, UUID ownerPatientId) {
        return new DietPlanProjection(command.dietPlanId(), type, ownerPatientId, true, command.startDate(), 15,
                IntStream.rangeClosed(1, 15).mapToObj(day -> new com.mcalvaro.mscatering.domain.dietplan.DietPlanDay(UUID.randomUUID(), day, 1,
                        List.of(new com.mcalvaro.mscatering.domain.dietplan.DietPlanMeal(UUID.randomUUID(), com.mcalvaro.mscatering.domain.dietplan.MealType.LUNCH, LocalTime.NOON,
                                List.of(new com.mcalvaro.mscatering.domain.dietplan.DietPlanRecipe(UUID.randomUUID(), "Meal", 1)))))).toList());
    }
}
