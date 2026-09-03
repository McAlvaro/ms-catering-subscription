package com.mcalvaro.mscatering.domain.subscription;

import com.mcalvaro.mscatering.domain.core.DomainException;
import com.mcalvaro.mscatering.domain.subscription.entity.BiweeklyEvaluation;
import com.mcalvaro.mscatering.domain.subscription.entity.DeliveryDay;
import com.mcalvaro.mscatering.domain.subscription.enums.DeliveryDayStatus;
import com.mcalvaro.mscatering.domain.subscription.enums.EvaluationStatus;
import com.mcalvaro.mscatering.domain.subscription.enums.ServiceType;
import com.mcalvaro.mscatering.domain.subscription.enums.SubscriptionStatus;
import com.mcalvaro.mscatering.domain.subscription.event.BiweeklyEvaluationCompleted;
import com.mcalvaro.mscatering.domain.subscription.event.DeliveryConfirmed;
import com.mcalvaro.mscatering.domain.subscription.event.DeliveryDayCancelled;
import com.mcalvaro.mscatering.domain.subscription.event.DeliveryDayModified;
import com.mcalvaro.mscatering.domain.subscription.event.DeliveryFailed;
import com.mcalvaro.mscatering.domain.subscription.event.SubscriptionCancelled;
import com.mcalvaro.mscatering.domain.subscription.event.SubscriptionCompleted;
import com.mcalvaro.mscatering.domain.subscription.event.SubscriptionCreated;
import com.mcalvaro.mscatering.domain.subscription.event.SubscriptionPaused;
import com.mcalvaro.mscatering.domain.subscription.event.SubscriptionReactivated;
import com.mcalvaro.mscatering.domain.subscription.vo.DeliveryAddress;
import com.mcalvaro.mscatering.domain.subscription.vo.DeliveryPreferences;
import com.mcalvaro.mscatering.domain.subscription.vo.PauseRange;
import com.mcalvaro.mscatering.domain.subscription.vo.ServiceContract;
import com.mcalvaro.mscatering.domain.subscription.vo.TimeWindow;
import com.mcalvaro.mscatering.domain.subscription.vo.ValidityPeriod;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SubscriptionTest {

    @Test
    @DisplayName("Should create an active subscription with one delivery for every contract day")
    void shouldCreateActiveSubscriptionWhenValidDetailsAreProvided() {
        // Arrange
        UUID subscriptionId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID dietPlanId = UUID.randomUUID();
        ServiceContract contract = buildContract(LocalDate.of(2099, 1, 1));
        DeliveryPreferences preferences = buildPreferences("Leave at reception");

        // Act
        Subscription subscription = Subscription.create(subscriptionId, patientId, dietPlanId, contract, preferences, 42);

        // Assert
        assertThat(subscription.getId()).isEqualTo(subscriptionId);
        assertThat(subscription.getPatientId()).isEqualTo(patientId);
        assertThat(subscription.getDietPlanId()).isEqualTo(dietPlanId);
        assertThat(subscription.getContract()).isEqualTo(contract);
        assertThat(subscription.getPreferences()).isEqualTo(preferences);
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(subscription.getDeliveryCalendar().getDeliveryDays())
                .hasSize(15)
                .allSatisfy(day -> {
                    assertThat(day.getStatus()).isEqualTo(DeliveryDayStatus.SCHEDULED);
                    assertThat(day.getAddress()).isEqualTo(preferences.primaryAddress());
                    assertThat(day.getTimeWindow()).isEqualTo(preferences.timeWindow());
                });
        assertThat(subscription.getDomainEvents()).singleElement()
                .isInstanceOfSatisfying(SubscriptionCreated.class, event -> {
                    assertThat(event.subscriptionId()).isEqualTo(subscriptionId);
                    assertThat(event.patientId()).isEqualTo(patientId);
                    assertThat(event.contractCode()).isEqualTo(subscription.getContractCode());
                });
    }

    @Test
    @DisplayName("Should pause and reactivate deliveries in the requested future range")
    void shouldReactivateSubscriptionWhenAnActivePauseIsEndedEarly() {
        // Arrange
        Subscription subscription = buildSubscription(LocalDate.of(2099, 1, 1));
        PauseRange range = new PauseRange(LocalDate.of(2099, 1, 4), LocalDate.of(2099, 1, 6));
        LocalDate reactivationDate = LocalDate.of(2099, 1, 5);
        subscription.clearDomainEvents();

        // Act
        subscription.pause(range, "Patient travelling");
        subscription.reactivate(reactivationDate);

        // Assert
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(subscription.getPauseRequests()).singleElement().satisfies(request -> {
            assertThat(request.isActive()).isFalse();
            assertThat(request.getActualEndDate()).isEqualTo(reactivationDate);
        });
        assertThat(subscription.getDeliveryCalendar().getDaysForDate(LocalDate.of(2099, 1, 4)))
                .singleElement().extracting(DeliveryDay::getStatus).isEqualTo(DeliveryDayStatus.SCHEDULED);
        assertThat(subscription.getDomainEvents()).hasSize(2);
        assertThat(subscription.getDomainEvents().get(0)).isInstanceOfSatisfying(SubscriptionPaused.class, event -> {
            assertThat(event.subscriptionId()).isEqualTo(subscription.getId());
            assertThat(event.pauseRange()).isEqualTo(range);
        });
        assertThat(subscription.getDomainEvents().get(1)).isInstanceOfSatisfying(SubscriptionReactivated.class, event -> {
            assertThat(event.subscriptionId()).isEqualTo(subscription.getId());
            assertThat(event.reactivatedOn()).isEqualTo(reactivationDate);
        });
    }

    @Test
    @DisplayName("Should update preferences and process delivery outcomes when delivery identifiers exist")
    void shouldUpdatePreferencesAndProcessDeliveryOutcomesWhenDaysExist() {
        // Arrange
        Subscription subscription = buildSubscription(LocalDate.of(2099, 2, 1));
        DeliveryPreferences newPreferences = buildPreferences("Ring the bell twice");
        DeliveryDay deliveredDay = subscription.getDeliveryCalendar().getDeliveryDays().get(0);
        DeliveryDay failedDay = subscription.getDeliveryCalendar().getDeliveryDays().get(1);
        DeliveryDay noDeliveryDay = subscription.getDeliveryCalendar().getDeliveryDays().get(2);
        String failureReason = "Recipient unavailable";
        subscription.clearDomainEvents();

        // Act
        subscription.updateDeliveryPreferences(newPreferences);
        subscription.confirmDelivery(deliveredDay.getId());
        subscription.registerFailedDelivery(failedDay.getId(), failureReason);
        subscription.markNoDelivery(noDeliveryDay.getId());

        // Assert
        assertThat(subscription.getPreferences()).isEqualTo(newPreferences);
        assertThat(deliveredDay.getStatus()).isEqualTo(DeliveryDayStatus.DELIVERED);
        assertThat(failedDay.getStatus()).isEqualTo(DeliveryDayStatus.FAILED);
        assertThat(failedDay.getFailureReason()).isEqualTo(failureReason);
        assertThat(noDeliveryDay.getStatus()).isEqualTo(DeliveryDayStatus.NOT_DELIVERED);
        assertThat(subscription.getDomainEvents()).hasSize(3);
        assertThat(subscription.getDomainEvents().get(0)).isInstanceOfSatisfying(DeliveryConfirmed.class, event -> {
            assertThat(event.subscriptionId()).isEqualTo(subscription.getId());
            assertThat(event.deliveryDayId()).isEqualTo(deliveredDay.getId());
        });
        assertThat(subscription.getDomainEvents().get(1)).isInstanceOfSatisfying(DeliveryFailed.class, event -> {
            assertThat(event.subscriptionId()).isEqualTo(subscription.getId());
            assertThat(event.deliveryDayId()).isEqualTo(failedDay.getId());
            assertThat(event.reason()).isEqualTo(failureReason);
        });
        assertThat(subscription.getDomainEvents().get(2)).isInstanceOfSatisfying(DeliveryDayCancelled.class, event -> {
            assertThat(event.subscriptionId()).isEqualTo(subscription.getId());
            assertThat(event.deliveryDayId()).isEqualTo(noDeliveryDay.getId());
        });
    }

    @Test
    @DisplayName("Should complete an evaluation and cancel only pending evaluations when subscription is cancelled")
    void shouldCompleteAndCancelEvaluationsWhenTheirOperationsAreRequested() {
        // Arrange
        Subscription subscription = buildSubscription(LocalDate.of(2099, 3, 1));
        BiweeklyEvaluation completed = new BiweeklyEvaluation(UUID.randomUUID(), subscription.getPatientId(), 1,
                LocalDate.of(2099, 3, 15));
        BiweeklyEvaluation pending = new BiweeklyEvaluation(UUID.randomUUID(), subscription.getPatientId(), 2,
                LocalDate.of(2099, 3, 29));
        LocalDate completedAt = LocalDate.of(2099, 3, 16);
        String cancellationReason = "Patient requested cancellation";
        subscription.scheduleEvaluations(java.util.List.of(completed, pending));
        subscription.clearDomainEvents();

        // Act
        subscription.markEvaluationCompleted(completed.getId(), completedAt);
        subscription.cancel(cancellationReason);

        // Assert
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
        assertThat(completed.getStatus()).isEqualTo(EvaluationStatus.COMPLETED);
        assertThat(completed.getCompletedDate()).isEqualTo(completedAt);
        assertThat(pending.getStatus()).isEqualTo(EvaluationStatus.CANCELLED);
        assertThat(subscription.getDeliveryCalendar().getDeliveryDays())
                .allSatisfy(day -> assertThat(day.getStatus()).isEqualTo(DeliveryDayStatus.CANCELLED));
        assertThat(subscription.getDomainEvents()).hasSize(2);
        assertThat(subscription.getDomainEvents().get(0)).isInstanceOfSatisfying(BiweeklyEvaluationCompleted.class,
                event -> {
                    assertThat(event.subscriptionId()).isEqualTo(subscription.getId());
                    assertThat(event.evaluationId()).isEqualTo(completed.getId());
                    assertThat(event.completedAt()).isEqualTo(completedAt);
                });
        assertThat(subscription.getDomainEvents().get(1)).isInstanceOfSatisfying(SubscriptionCancelled.class, event -> {
            assertThat(event.subscriptionId()).isEqualTo(subscription.getId());
            assertThat(event.reason()).isEqualTo(cancellationReason);
        });
    }

    @Test
    @DisplayName("Should complete an active subscription after its contract period has ended")
    void shouldCompleteSubscriptionWhenContractPeriodEndedInThePast() {
        // Arrange
        Subscription subscription = buildSubscription(LocalDate.of(2020, 1, 1));
        subscription.clearDomainEvents();

        // Act
        subscription.complete();

        // Assert
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.COMPLETED);
        assertThat(subscription.getDomainEvents()).singleElement()
                .isInstanceOfSatisfying(SubscriptionCompleted.class,
                        event -> assertThat(event.subscriptionId()).isEqualTo(subscription.getId()));
    }

    @Test
    @DisplayName("Should modify a delivery day more than 48 hours in advance and emit its event")
    void shouldModifyDeliveryDayWhenItIsMoreThanFortyEightHoursInTheFuture() {
        // Arrange
        Subscription subscription = buildSubscription(LocalDate.of(2099, 9, 1));
        DeliveryDay deliveryDay = subscription.getDeliveryCalendar().getDeliveryDays().get(0);
        DeliveryAddress newAddress = new DeliveryAddress("New Street", "25", "Madrid", "Floor 2", 40.4200,
                -3.7000, "600000001");
        TimeWindow newTimeWindow = new TimeWindow(LocalTime.of(12, 0), LocalTime.of(14, 0));
        String newInstructions = "Call on arrival";
        subscription.clearDomainEvents();

        // Act
        subscription.modifyDeliveryDay(deliveryDay.getId(), newAddress, newTimeWindow, newInstructions);

        // Assert
        assertThat(deliveryDay.getAddress()).isEqualTo(newAddress);
        assertThat(deliveryDay.getTimeWindow()).isEqualTo(newTimeWindow);
        assertThat(deliveryDay.getInstructions()).isEqualTo(newInstructions);
        assertThat(subscription.getDomainEvents()).singleElement()
                .isInstanceOfSatisfying(DeliveryDayModified.class, event -> {
                    assertThat(event.subscriptionId()).isEqualTo(subscription.getId());
                    assertThat(event.deliveryDayId()).isEqualTo(deliveryDay.getId());
                    assertThat(event.date()).isEqualTo(deliveryDay.getDate());
                });
    }

    @Test
    @DisplayName("Should expose unmodifiable pause requests and evaluations collections")
    void shouldExposeUnmodifiableCollectionsWhenAggregateContainsEntries() {
        // Arrange
        Subscription subscription = buildSubscription(LocalDate.of(2099, 4, 1));
        subscription.pause(new PauseRange(LocalDate.of(2099, 4, 4), LocalDate.of(2099, 4, 6)), "Medical leave");
        subscription.scheduleEvaluations(java.util.List.of(new BiweeklyEvaluation(UUID.randomUUID(), subscription.getPatientId(),
                1, LocalDate.of(2099, 4, 15))));

        // Act & Assert
        assertThatThrownBy(() -> subscription.getPauseRequests().clear())
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> subscription.getEvaluations().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("Should throw DomainException SUB-003 when pausing a subscription that is not active")
    void shouldThrowDomainExceptionWhenPausingSubscriptionThatIsAlreadyPaused() {
        // Arrange
        Subscription subscription = buildSubscription(LocalDate.of(2099, 5, 1));
        subscription.pause(new PauseRange(LocalDate.of(2099, 5, 4), LocalDate.of(2099, 5, 6)), "Travel");

        // Act & Assert
        assertThatThrownBy(() -> subscription.pause(
                new PauseRange(LocalDate.of(2099, 5, 7), LocalDate.of(2099, 5, 9)), "Extension"))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("code", "SUB-003");
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.PAUSED);
        assertThat(subscription.getPauseRequests()).hasSize(1);
    }

    @Test
    @DisplayName("Should throw DomainException SUB-004 when reactivating an active subscription")
    void shouldThrowDomainExceptionWhenReactivatingSubscriptionThatIsActive() {
        // Arrange
        Subscription subscription = buildSubscription(LocalDate.of(2099, 6, 1));

        // Act & Assert
        assertThatThrownBy(() -> subscription.reactivate(LocalDate.of(2099, 6, 4)))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("code", "SUB-004");
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    }

    @Test
    @DisplayName("Should throw DomainException SUB-008 when completing before a future contract period ends")
    void shouldThrowDomainExceptionWhenCompletingBeforeContractPeriodEnds() {
        // Arrange
        Subscription subscription = buildSubscription(LocalDate.of(2099, 7, 1));

        // Act & Assert
        assertThatThrownBy(subscription::complete)
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("code", "SUB-008");
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    }

    @Test
    @DisplayName("Should throw DomainException SUB-009 when confirming an unknown delivery day")
    void shouldThrowDomainExceptionWhenConfirmingUnknownDeliveryDay() {
        // Arrange
        Subscription subscription = buildSubscription(LocalDate.of(2099, 8, 1));
        subscription.clearDomainEvents();

        // Act & Assert
        assertThatThrownBy(() -> subscription.confirmDelivery(UUID.randomUUID()))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("code", "SUB-009");
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(subscription.getDomainEvents()).isEmpty();
    }

    @Test
    @DisplayName("Should throw DomainException SUB-012 and preserve the last active delivery day")
    void shouldThrowDomainExceptionWhenMarkingTheOnlyActiveDeliveryAsNoDelivery() {
        // Arrange
        Subscription subscription = buildSubscription(LocalDate.of(2099, 10, 1));
        java.util.List<DeliveryDay> deliveryDays = subscription.getDeliveryCalendar().getDeliveryDays();
        deliveryDays.subList(0, deliveryDays.size() - 1)
                .forEach(deliveryDay -> subscription.markNoDelivery(deliveryDay.getId()));
        DeliveryDay lastActiveDay = deliveryDays.get(deliveryDays.size() - 1);
        subscription.clearDomainEvents();

        // Act & Assert
        assertThatThrownBy(() -> subscription.markNoDelivery(lastActiveDay.getId()))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("code", "SUB-012");
        assertThat(lastActiveDay.getStatus()).isEqualTo(DeliveryDayStatus.SCHEDULED);
        assertThat(subscription.getDomainEvents()).isEmpty();
    }

    @Test
    @DisplayName("Should reject completion of an evaluation that does not belong to the subscription")
    void shouldThrowIllegalArgumentExceptionWhenEvaluationDoesNotExist() {
        // Arrange
        Subscription subscription = buildSubscription(LocalDate.of(2099, 11, 1));
        BiweeklyEvaluation scheduledEvaluation = new BiweeklyEvaluation(UUID.randomUUID(), subscription.getPatientId(), 1,
                LocalDate.of(2099, 11, 15));
        subscription.scheduleEvaluations(java.util.List.of(scheduledEvaluation));
        subscription.clearDomainEvents();

        // Act & Assert
        assertThatThrownBy(() -> subscription.markEvaluationCompleted(UUID.randomUUID(), LocalDate.of(2099, 11, 16)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Evaluation not found");
        assertThat(scheduledEvaluation.getStatus()).isEqualTo(EvaluationStatus.PENDING);
        assertThat(subscription.getDomainEvents()).isEmpty();
    }

    @Test
    @DisplayName("Should throw DomainException SUB-003 when completing a cancelled subscription")
    void shouldThrowDomainExceptionWhenCompletingSubscriptionThatIsNotActive() {
        // Arrange
        Subscription subscription = buildSubscription(LocalDate.of(2020, 1, 1));
        subscription.cancel("Patient requested cancellation");
        subscription.clearDomainEvents();

        // Act & Assert
        assertThatThrownBy(subscription::complete)
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("code", "SUB-003");
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
        assertThat(subscription.getDomainEvents()).isEmpty();
    }

    private Subscription buildSubscription(LocalDate startDate) {
        return Subscription.create(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), buildContract(startDate),
                buildPreferences("Leave at reception"), 1);
    }

    private ServiceContract buildContract(LocalDate startDate) {
        return new ServiceContract(UUID.randomUUID(), new ValidityPeriod(startDate, startDate.plusDays(14)),
                ServiceType.FULL, new BigDecimal("250.00"), "Terms accepted", Instant.parse("2025-01-01T10:00:00Z"));
    }

    private DeliveryPreferences buildPreferences(String instructions) {
        return new DeliveryPreferences(
                new DeliveryAddress("Main Street", "10", "Madrid", "Door A", 40.4168, -3.7038, "600000000"),
                new TimeWindow(LocalTime.of(9, 0), LocalTime.of(11, 0)), instructions);
    }
}
