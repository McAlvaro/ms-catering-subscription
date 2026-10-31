package com.mcalvaro.mscatering.domain.subscription;

import com.mcalvaro.mscatering.domain.core.CorrelatedDomainEvent;
import com.mcalvaro.mscatering.domain.core.DomainException;
import com.mcalvaro.mscatering.domain.core.id.DietPlanId;
import com.mcalvaro.mscatering.domain.core.id.PatientId;
import com.mcalvaro.mscatering.domain.core.id.SubscriptionId;
import com.mcalvaro.mscatering.domain.subscription.entity.DeliveryDay;
import com.mcalvaro.mscatering.domain.subscription.enums.DeliveryDayStatus;
import com.mcalvaro.mscatering.domain.subscription.enums.ServiceType;
import com.mcalvaro.mscatering.domain.subscription.enums.SubscriptionStatus;
import com.mcalvaro.mscatering.domain.subscription.event.SubscriptionCancelled;
import com.mcalvaro.mscatering.domain.subscription.event.SubscriptionCompleted;
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

class SubscriptionOutboundLifecycleTest {

    @Test
    @DisplayName("Should accept a one-day pause, extend both periods and snapshot its correlation")
    void shouldPauseForOneInclusiveDayWhenRangeIsWithinTheEffectiveValidity() {
        // Arrange
        Subscription subscription = buildSubscription(LocalDate.now());
        LocalDate pauseDay = LocalDate.now().plusDays(12);
        LocalDate previousEnd = subscription.getContract().period().endDate();
        String correlationId = "pause-flow-123";
        subscription.clearDomainEvents();

        // Act
        subscription.pause(new PauseRange(pauseDay, pauseDay), "Medical leave", correlationId);

        // Assert
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.PAUSED);
        assertThat(subscription.getContract().period().endDate()).isEqualTo(previousEnd.plusDays(1));
        assertThat(subscription.getDeliveryCalendar().getPeriod().endDate()).isEqualTo(previousEnd.plusDays(1));
        assertThat(subscription.getDeliveryCalendar().getDeliveryDays()).hasSize(16);
        assertThat(subscription.getDeliveryCalendar().getDaysForDate(pauseDay))
                .singleElement().extracting(DeliveryDay::getStatus).isEqualTo(DeliveryDayStatus.PAUSED);
        assertThat(subscription.getPauseRequests()).singleElement().satisfies(request -> {
            assertThat(request.getCorrelationId()).isEqualTo(correlationId);
            assertThat(request.getExtensionDays()).isEqualTo(1);
        });
        assertThat(subscription.getDomainEvents()).singleElement().isInstanceOfSatisfying(SubscriptionPaused.class, event -> {
            assertThat(event.patientId()).isEqualTo(subscription.getPatientId());
            assertThat(event.reason()).isEqualTo("Medical leave");
            assertThat(event.requestedOn()).isEqualTo(LocalDate.now());
            assertThat(event.newEndDate()).isEqualTo(previousEnd.plusDays(1));
            assertThat(event.correlationId()).isEqualTo(correlationId);
            assertThat(event).isInstanceOf(CorrelatedDomainEvent.class);
        });
    }

    @Test
    @DisplayName("Should reject a pause atomically when an affected day is already consolidated")
    void shouldRejectPauseAtomicallyWhenAffectedDayIsProcessed() {
        // Arrange
        Subscription subscription = buildFutureSubscription();
        LocalDate pauseDay = LocalDate.now().plusDays(12);
        LocalDate previousEnd = subscription.getContract().period().endDate();
        subscription.getDeliveryCalendar().getDaysForDate(pauseDay).get(0).markAsConsolidated();
        subscription.clearDomainEvents();

        // Act & Assert
        assertThatThrownBy(() -> subscription.pause(new PauseRange(pauseDay, pauseDay), "Medical leave", "corr-1"))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("code", "SUB-011");
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(subscription.getContract().period().endDate()).isEqualTo(previousEnd);
        assertThat(subscription.getDeliveryCalendar().getDaysForDate(pauseDay))
                .singleElement().extracting(DeliveryDay::getStatus).isEqualTo(DeliveryDayStatus.CONSOLIDATED);
        assertThat(subscription.getPauseRequests()).isEmpty();
        assertThat(subscription.getDomainEvents()).isEmpty();
    }

    @Test
    @DisplayName("Should reject a pause whose inclusive end date exceeds the current effective validity")
    void shouldRejectPauseWhenEndDateIsOutsideEffectiveValidity() {
        // Arrange
        Subscription subscription = buildFutureSubscription();
        LocalDate currentEnd = subscription.getContract().period().endDate();
        PauseRange range = new PauseRange(currentEnd.minusDays(1), currentEnd.plusDays(1));
        subscription.clearDomainEvents();

        // Act & Assert
        assertThatThrownBy(() -> subscription.pause(range, "Travel", "corr-outside-validity"))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("code", "SUB-013");
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(subscription.getContract().period().endDate()).isEqualTo(currentEnd);
        assertThat(subscription.getPauseRequests()).isEmpty();
        assertThat(subscription.getDomainEvents()).isEmpty();
    }

    @Test
    @DisplayName("Should reactivate only the remaining paused days and remove unprocessed trailing extensions")
    void shouldReactivateOnlyRemainingPausedDaysWhenNoticeIsValid() {
        // Arrange
        Subscription subscription = buildFutureSubscription();
        LocalDate pauseStart = LocalDate.now().plusDays(12);
        LocalDate pauseEnd = pauseStart.plusDays(2);
        LocalDate reactivationDate = pauseStart.plusDays(1);
        LocalDate originalEnd = subscription.getContract().period().endDate();
        subscription.pause(new PauseRange(pauseStart, pauseEnd), "Travel", "persisted-pause-correlation");
        subscription.clearDomainEvents();

        // Act
        subscription.reactivate(reactivationDate);

        // Assert
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(subscription.getContract().period().endDate()).isEqualTo(originalEnd.plusDays(1));
        assertThat(subscription.getDeliveryCalendar().getPeriod().endDate()).isEqualTo(originalEnd.plusDays(1));
        assertThat(subscription.getDeliveryCalendar().getDeliveryDays()).hasSize(16);
        assertThat(subscription.getDeliveryCalendar().getDaysForDate(pauseStart))
                .singleElement().extracting(DeliveryDay::getStatus).isEqualTo(DeliveryDayStatus.PAUSED);
        assertThat(subscription.getDeliveryCalendar().getDaysForDate(reactivationDate))
                .singleElement().extracting(DeliveryDay::getStatus).isEqualTo(DeliveryDayStatus.SCHEDULED);
        assertThat(subscription.getDomainEvents()).singleElement().isInstanceOfSatisfying(SubscriptionReactivated.class,
                event -> {
                    assertThat(event.patientId()).isEqualTo(subscription.getPatientId());
                    assertThat(event.effectiveEndDate()).isEqualTo(originalEnd.plusDays(1));
                    assertThat(event.correlationId()).isEqualTo("persisted-pause-correlation");
                });
    }

    @Test
    @DisplayName("Should reject reactivation requested with less than forty-eight hours notice")
    void shouldRejectReactivationWhenNoticeIsLessThanFortyEightHours() {
        // Arrange
        Subscription subscription = buildSubscription(LocalDate.now());
        LocalDate pauseStart = LocalDate.now().plusDays(2);
        subscription.pause(new PauseRange(pauseStart, pauseStart.plusDays(2)), "Travel", "pause-correlation");
        LocalDate extendedEnd = subscription.getContract().period().endDate();
        subscription.clearDomainEvents();

        // Act & Assert
        assertThatThrownBy(() -> subscription.reactivate(LocalDate.now().plusDays(1)))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("code", "SUB-001");
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.PAUSED);
        assertThat(subscription.getContract().period().endDate()).isEqualTo(extendedEnd);
        assertThat(subscription.getDomainEvents()).isEmpty();
    }

    @Test
    @DisplayName("Should reject reactivation without mutating the pause when a trailing extension was processed")
    void shouldRejectReactivationAtomicallyWhenTrailingExtensionWasProcessed() {
        // Arrange
        Subscription subscription = buildFutureSubscription();
        LocalDate pauseStart = LocalDate.now().plusDays(12);
        LocalDate pauseEnd = pauseStart.plusDays(2);
        LocalDate originalEnd = subscription.getContract().period().endDate();
        subscription.pause(new PauseRange(pauseStart, pauseEnd), "Travel", "persisted-pause-correlation");
        subscription.getDeliveryCalendar().getDaysForDate(originalEnd.plusDays(3)).get(0).markAsConsolidated();
        subscription.clearDomainEvents();

        // Act & Assert
        assertThatThrownBy(() -> subscription.reactivate(pauseStart.plusDays(1)))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("code", "SUB-011");
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.PAUSED);
        assertThat(subscription.getContract().period().endDate()).isEqualTo(originalEnd.plusDays(3));
        assertThat(subscription.getDeliveryCalendar().getDaysForDate(pauseStart.plusDays(1)))
                .singleElement().extracting(DeliveryDay::getStatus).isEqualTo(DeliveryDayStatus.PAUSED);
        assertThat(subscription.getDomainEvents()).isEmpty();
    }

    @Test
    @DisplayName("Should snapshot cancellation and completion facts before their delivery days are mutated")
    void shouldSnapshotTerminalLifecycleFactsBeforeDeliveryMutation() {
        // Arrange
        Subscription cancellable = buildFutureSubscription();
        cancellable.getDeliveryCalendar().getDeliveryDays().get(0).markAsDelivered();
        cancellable.clearDomainEvents();
        Subscription completable = buildPastSubscription();
        completable.clearDomainEvents();

        // Act
        cancellable.cancel("Patient request", "cancel-correlation");
        completable.complete("complete-correlation");

        // Assert
        assertThat(cancellable.getDomainEvents()).singleElement().isInstanceOfSatisfying(SubscriptionCancelled.class,
                event -> {
                    assertThat(event.patientId()).isEqualTo(cancellable.getPatientId());
                    assertThat(event.dietPlanId()).isEqualTo(cancellable.getDietPlanId());
                    assertThat(event.effectiveDate()).isEqualTo(LocalDate.now());
                    assertThat(event.cancelledPendingDeliveries()).isEqualTo(14);
                    assertThat(event.correlationId()).isEqualTo("cancel-correlation");
                });
        assertThat(completable.getDomainEvents()).singleElement().isInstanceOfSatisfying(SubscriptionCompleted.class,
                event -> {
                    assertThat(event.patientId()).isEqualTo(completable.getPatientId());
                    assertThat(event.dietPlanId()).isEqualTo(completable.getDietPlanId());
                    assertThat(event.effectiveDate()).isEqualTo(completable.getContract().period().endDate());
                    assertThat(event.cancelledPendingDeliveries()).isZero();
                    assertThat(event.correlationId()).isEqualTo("complete-correlation");
                });
    }

    @Test
    @DisplayName("Should reject a blank cancellation reason without mutating the aggregate")
    void shouldRejectBlankCancellationReasonWithoutMutation() {
        // Arrange
        Subscription subscription = buildFutureSubscription();
        subscription.clearDomainEvents();

        // Act & Assert
        assertThatThrownBy(() -> subscription.cancel(" ", "cancel-correlation"))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("code", "SUB-014");
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(subscription.getDeliveryCalendar().getDeliveryDays())
                .allSatisfy(day -> assertThat(day.getStatus()).isEqualTo(DeliveryDayStatus.SCHEDULED));
        assertThat(subscription.getDomainEvents()).isEmpty();
    }

    private Subscription buildFutureSubscription() {
        LocalDate start = LocalDate.now().plusDays(10);
        return buildSubscription(start);
    }

    private Subscription buildPastSubscription() {
        return buildSubscription(LocalDate.now().minusDays(20));
    }

    private Subscription buildSubscription(LocalDate start) {
        return Subscription.create(SubscriptionId.generate(), PatientId.generate(), DietPlanId.generate(),
                new ServiceContract(DietPlanId.generate(), com.mcalvaro.mscatering.domain.dietplan.PlanType.BASE, new ValidityPeriod(start, start.plusDays(14)), ServiceType.FULL,
                        new BigDecimal("250.00"), "Terms accepted", Instant.parse("2025-01-01T10:00:00Z")),
                new DeliveryPreferences(
                        new DeliveryAddress("Main Street", "10", "Madrid", "Door A", 40.4168, -3.7038,
                                "600000000"),
                        new TimeWindow(LocalTime.of(9, 0), LocalTime.of(11, 0)), "Leave at reception"),
                1);
    }
}
