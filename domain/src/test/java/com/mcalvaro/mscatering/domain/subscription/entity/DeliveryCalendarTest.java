package com.mcalvaro.mscatering.domain.subscription.entity;

import com.mcalvaro.mscatering.domain.subscription.enums.DeliveryDayStatus;
import com.mcalvaro.mscatering.domain.subscription.vo.DeliveryAddress;
import com.mcalvaro.mscatering.domain.subscription.vo.PauseRange;
import com.mcalvaro.mscatering.domain.subscription.vo.TimeWindow;
import com.mcalvaro.mscatering.domain.subscription.vo.ValidityPeriod;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeliveryCalendarTest {

    @Test
    @DisplayName("Should create a calendar with its identifiers, period and no delivery days")
    void shouldCreateCalendarWhenValidDetailsAreProvided() {
        // Arrange
        UUID calendarId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();
        ValidityPeriod period = new ValidityPeriod(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));

        // Act
        DeliveryCalendar calendar = new DeliveryCalendar(calendarId, subscriptionId, period);

        // Assert
        assertThat(calendar.getId()).isEqualTo(calendarId);
        assertThat(calendar.getSubscriptionId()).isEqualTo(subscriptionId);
        assertThat(calendar.getPeriod()).isEqualTo(period);
        assertThat(calendar.getDeliveryDays()).isEmpty();
    }

    @Test
    @DisplayName("Should add a delivery day and expose an unmodifiable days list")
    void shouldAddDayAndExposeUnmodifiableDaysWhenDayIsAdded() {
        // Arrange
        DeliveryCalendar calendar = newCalendar();
        DeliveryDay day = newDay(UUID.randomUUID(), LocalDate.of(2026, 1, 10));

        // Act
        calendar.addDay(day);

        // Assert
        assertThat(calendar.getDeliveryDays()).containsExactly(day);
        assertThatThrownBy(() -> calendar.getDeliveryDays().add(newDay(UUID.randomUUID(), LocalDate.of(2026, 1, 11))))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("Should pause only scheduled days inside the inclusive pause range")
    void shouldPauseOnlyScheduledDaysWithinInclusiveRangeWhenRangeIsPaused() {
        // Arrange
        LocalDate start = LocalDate.now().plusDays(3);
        LocalDate end = start.plusDays(2);
        PauseRange range = new PauseRange(start, end);
        DeliveryDay atStart = newDay(UUID.randomUUID(), start);
        DeliveryDay atEnd = newDay(UUID.randomUUID(), end);
        DeliveryDay outsideRange = newDay(UUID.randomUUID(), end.plusDays(1));
        DeliveryDay alreadyPaused = newDayWithStatus(UUID.randomUUID(), start.plusDays(1), DeliveryDayStatus.PAUSED);
        DeliveryDay consolidated = newDayWithStatus(UUID.randomUUID(), start.plusDays(1), DeliveryDayStatus.CONSOLIDATED);
        DeliveryDay delivered = newDayWithStatus(UUID.randomUUID(), start.plusDays(1), DeliveryDayStatus.DELIVERED);
        DeliveryDay notDelivered = newDayWithStatus(UUID.randomUUID(), start.plusDays(1), DeliveryDayStatus.NOT_DELIVERED);
        DeliveryDay failed = newDayWithStatus(UUID.randomUUID(), start.plusDays(1), DeliveryDayStatus.FAILED);
        DeliveryDay cancelled = newDayWithStatus(UUID.randomUUID(), start.plusDays(1), DeliveryDayStatus.CANCELLED);
        DeliveryCalendar calendar = calendarWith(atStart, atEnd, outsideRange, alreadyPaused, consolidated, delivered,
                notDelivered, failed, cancelled);

        // Act
        calendar.pauseRange(range);

        // Assert
        assertThat(atStart.getStatus()).isEqualTo(DeliveryDayStatus.PAUSED);
        assertThat(atEnd.getStatus()).isEqualTo(DeliveryDayStatus.PAUSED);
        assertThat(outsideRange.getStatus()).isEqualTo(DeliveryDayStatus.SCHEDULED);
        assertThat(alreadyPaused.getStatus()).isEqualTo(DeliveryDayStatus.PAUSED);
        assertThat(consolidated.getStatus()).isEqualTo(DeliveryDayStatus.CONSOLIDATED);
        assertThat(delivered.getStatus()).isEqualTo(DeliveryDayStatus.DELIVERED);
        assertThat(notDelivered.getStatus()).isEqualTo(DeliveryDayStatus.NOT_DELIVERED);
        assertThat(failed.getStatus()).isEqualTo(DeliveryDayStatus.FAILED);
        assertThat(cancelled.getStatus()).isEqualTo(DeliveryDayStatus.CANCELLED);
    }

    @Test
    @DisplayName("Should reactivate only paused days inside the inclusive pause range")
    void shouldReactivateOnlyPausedDaysWithinInclusiveRangeWhenRangeIsReactivated() {
        // Arrange
        LocalDate start = LocalDate.now().plusDays(3);
        LocalDate end = start.plusDays(2);
        PauseRange range = new PauseRange(start, end);
        DeliveryDay atStart = newDayWithStatus(UUID.randomUUID(), start, DeliveryDayStatus.PAUSED);
        DeliveryDay atEnd = newDayWithStatus(UUID.randomUUID(), end, DeliveryDayStatus.PAUSED);
        DeliveryDay outsideRange = newDayWithStatus(UUID.randomUUID(), end.plusDays(1), DeliveryDayStatus.PAUSED);
        DeliveryDay scheduled = newDay(UUID.randomUUID(), start.plusDays(1));
        DeliveryDay consolidated = newDayWithStatus(UUID.randomUUID(), start.plusDays(1), DeliveryDayStatus.CONSOLIDATED);
        DeliveryDay delivered = newDayWithStatus(UUID.randomUUID(), start.plusDays(1), DeliveryDayStatus.DELIVERED);
        DeliveryDay notDelivered = newDayWithStatus(UUID.randomUUID(), start.plusDays(1), DeliveryDayStatus.NOT_DELIVERED);
        DeliveryDay failed = newDayWithStatus(UUID.randomUUID(), start.plusDays(1), DeliveryDayStatus.FAILED);
        DeliveryDay cancelled = newDayWithStatus(UUID.randomUUID(), start.plusDays(1), DeliveryDayStatus.CANCELLED);
        DeliveryCalendar calendar = calendarWith(atStart, atEnd, outsideRange, scheduled, consolidated, delivered,
                notDelivered, failed, cancelled);

        // Act
        calendar.reactivateRange(range);

        // Assert
        assertThat(atStart.getStatus()).isEqualTo(DeliveryDayStatus.SCHEDULED);
        assertThat(atEnd.getStatus()).isEqualTo(DeliveryDayStatus.SCHEDULED);
        assertThat(outsideRange.getStatus()).isEqualTo(DeliveryDayStatus.PAUSED);
        assertThat(scheduled.getStatus()).isEqualTo(DeliveryDayStatus.SCHEDULED);
        assertThat(consolidated.getStatus()).isEqualTo(DeliveryDayStatus.CONSOLIDATED);
        assertThat(delivered.getStatus()).isEqualTo(DeliveryDayStatus.DELIVERED);
        assertThat(notDelivered.getStatus()).isEqualTo(DeliveryDayStatus.NOT_DELIVERED);
        assertThat(failed.getStatus()).isEqualTo(DeliveryDayStatus.FAILED);
        assertThat(cancelled.getStatus()).isEqualTo(DeliveryDayStatus.CANCELLED);
    }

    @Test
    @DisplayName("Should find a delivery day when its identifier exists")
    void shouldFindDayWhenIdentifierExists() {
        // Arrange
        UUID dayId = UUID.randomUUID();
        DeliveryDay expectedDay = newDay(dayId, LocalDate.of(2026, 1, 10));
        DeliveryCalendar calendar = calendarWith(expectedDay);

        // Act
        var result = calendar.findDayById(dayId);

        // Assert
        assertThat(result).contains(expectedDay);
    }

    @Test
    @DisplayName("Should return an empty result when a delivery day identifier does not exist")
    void shouldReturnEmptyWhenDayIdentifierDoesNotExist() {
        // Arrange
        DeliveryCalendar calendar = calendarWith(newDay(UUID.randomUUID(), LocalDate.of(2026, 1, 10)));

        // Act
        var result = calendar.findDayById(UUID.randomUUID());

        // Assert
        assertThat(result).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(value = DeliveryDayStatus.class, names = {"SCHEDULED", "PAUSED", "CONSOLIDATED", "DELIVERED"})
    @DisplayName("Should retain an active day after excluding a different delivery day for every active status")
    void shouldRetainActiveDayWhenDifferentDayIsExcludedForEveryActiveStatus(DeliveryDayStatus status) {
        // Arrange
        DeliveryDay activeDay = newDayWithStatus(UUID.randomUUID(), LocalDate.of(2026, 1, 10), status);
        DeliveryCalendar calendar = calendarWith(activeDay);

        // Act
        boolean hasActiveDays = calendar.hasActiveDaysAfterExcluding(UUID.randomUUID());

        // Assert
        assertThat(hasActiveDays).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = DeliveryDayStatus.class, names = {"NOT_DELIVERED", "FAILED", "CANCELLED"})
    @DisplayName("Should not consider negative statuses as active days")
    void shouldNotRetainActiveDayWhenOnlyNegativeStatusExists(DeliveryDayStatus status) {
        // Arrange
        DeliveryDay inactiveDay = newDayWithStatus(UUID.randomUUID(), LocalDate.of(2026, 1, 10), status);
        DeliveryCalendar calendar = calendarWith(inactiveDay);

        // Act
        boolean hasActiveDays = calendar.hasActiveDaysAfterExcluding(UUID.randomUUID());

        // Assert
        assertThat(hasActiveDays).isFalse();
    }

    @Test
    @DisplayName("Should not retain an active day when the only active day is excluded")
    void shouldNotRetainActiveDayWhenOnlyActiveDayIsExcluded() {
        // Arrange
        UUID dayId = UUID.randomUUID();
        DeliveryCalendar calendar = calendarWith(newDay(dayId, LocalDate.of(2026, 1, 10)));

        // Act
        boolean hasActiveDays = calendar.hasActiveDaysAfterExcluding(dayId);

        // Assert
        assertThat(hasActiveDays).isFalse();
    }

    @Test
    @DisplayName("Should return only delivery days matching the requested date")
    void shouldReturnOnlyDaysForDateWhenCalendarContainsMultipleDates() {
        // Arrange
        LocalDate requestedDate = LocalDate.of(2026, 1, 10);
        DeliveryDay firstMatchingDay = newDay(UUID.randomUUID(), requestedDate);
        DeliveryDay secondMatchingDay = newDay(UUID.randomUUID(), requestedDate);
        DeliveryDay otherDay = newDay(UUID.randomUUID(), requestedDate.plusDays(1));
        DeliveryCalendar calendar = calendarWith(firstMatchingDay, secondMatchingDay, otherDay);

        // Act
        List<DeliveryDay> daysForDate = calendar.getDaysForDate(requestedDate);

        // Assert
        assertThat(daysForDate).containsExactly(firstMatchingDay, secondMatchingDay);
    }

    @Test
    @DisplayName("Should return no delivery days when the requested date has no matches")
    void shouldReturnNoDaysWhenRequestedDateHasNoMatches() {
        // Arrange
        DeliveryCalendar calendar = calendarWith(newDay(UUID.randomUUID(), LocalDate.of(2026, 1, 10)));

        // Act
        List<DeliveryDay> daysForDate = calendar.getDaysForDate(LocalDate.of(2026, 1, 11));

        // Assert
        assertThat(daysForDate).isEmpty();
    }

    private DeliveryCalendar newCalendar() {
        return new DeliveryCalendar(UUID.randomUUID(), UUID.randomUUID(),
                new ValidityPeriod(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)));
    }

    private DeliveryCalendar calendarWith(DeliveryDay... days) {
        DeliveryCalendar calendar = newCalendar();
        for (DeliveryDay day : days) {
            calendar.addDay(day);
        }
        return calendar;
    }

    private DeliveryDay newDay(UUID id, LocalDate date) {
        return new DeliveryDay(id, date,
                new DeliveryAddress("Main Street", "10", "Madrid", "Door A", 40.4168, -3.7038, "600000000"),
                new TimeWindow(LocalTime.of(9, 0), LocalTime.of(11, 0)), "Leave at reception");
    }

    private DeliveryDay newDayWithStatus(UUID id, LocalDate date, DeliveryDayStatus status) {
        DeliveryDay day = newDay(id, date);
        switch (status) {
            case PAUSED -> day.pause();
            case CONSOLIDATED -> day.markAsConsolidated();
            case DELIVERED -> day.markAsDelivered();
            case NOT_DELIVERED -> day.markAsNoDelivery();
            case FAILED -> day.markAsFailed("Recipient unavailable");
            case CANCELLED -> day.cancel();
            case SCHEDULED -> {
            }
        }
        return day;
    }
}
