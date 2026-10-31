package com.mcalvaro.mscatering.domain.subscription.entity;

import com.mcalvaro.mscatering.domain.core.Entity;
import com.mcalvaro.mscatering.domain.core.id.DeliveryCalendarId;
import com.mcalvaro.mscatering.domain.core.id.DeliveryDayId;
import com.mcalvaro.mscatering.domain.core.id.SubscriptionId;
import com.mcalvaro.mscatering.domain.subscription.enums.DeliveryDayStatus;
import com.mcalvaro.mscatering.domain.subscription.vo.PauseRange;
import com.mcalvaro.mscatering.domain.subscription.vo.DeliveryAddress;
import com.mcalvaro.mscatering.domain.subscription.vo.TimeWindow;
import com.mcalvaro.mscatering.domain.subscription.vo.ValidityPeriod;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Entity that manages the full collection of delivery days for a subscription
 * period (CalendarioDeEntregas).
 */
public class DeliveryCalendar extends Entity<DeliveryCalendarId> {

    private final SubscriptionId subscriptionId;
    private ValidityPeriod period;
    private final List<DeliveryDay> deliveryDays = new ArrayList<>();

    public DeliveryCalendar(DeliveryCalendarId id, SubscriptionId subscriptionId, ValidityPeriod period) {
        super(id);
        this.subscriptionId = subscriptionId;
        this.period = period;
    }

    public void addDay(DeliveryDay day) {
        deliveryDays.add(day);
    }

    /**
     * Pauses all {@code SCHEDULED} delivery days whose date falls within the range.
     */
    public void pauseRange(PauseRange range) {
        deliveryDays.stream()
                .filter(d -> d.getStatus() == DeliveryDayStatus.SCHEDULED)
                .filter(d -> !d.getDate().isBefore(range.startDate())
                        && !d.getDate().isAfter(range.endDate()))
                .forEach(DeliveryDay::pause);
    }

    /**
     * Reactivates all {@code PAUSED} delivery days within the range.
     */
    public void reactivateRange(PauseRange range) {
        deliveryDays.stream()
                .filter(d -> d.getStatus() == DeliveryDayStatus.PAUSED)
                .filter(d -> !d.getDate().isBefore(range.startDate())
                        && !d.getDate().isAfter(range.endDate()))
                .forEach(DeliveryDay::reactivate);
    }

    public boolean hasProcessedDays(PauseRange range) {
        return deliveryDays.stream()
                .filter(d -> !d.getDate().isBefore(range.startDate()) && !d.getDate().isAfter(range.endDate()))
                .anyMatch(this::isProcessed);
    }

    public boolean hasProcessedDaysAfter(LocalDate date) {
        return deliveryDays.stream()
                .filter(d -> d.getDate().isAfter(date))
                .anyMatch(this::isProcessed);
    }

    public void extendTo(ValidityPeriod newPeriod, DeliveryAddress address, TimeWindow timeWindow, String instructions) {
        LocalDate previousEnd = period.endDate();
        for (LocalDate day : previousEnd.plusDays(1).datesUntil(newPeriod.endDate().plusDays(1)).toList()) {
            deliveryDays.add(new DeliveryDay(DeliveryDayId.generate(), day, address, timeWindow, instructions));
        }
        period = newPeriod;
    }

    public void removeDaysAfter(LocalDate endDate) {
        deliveryDays.removeIf(day -> day.getDate().isAfter(endDate));
        period = new ValidityPeriod(period.startDate(), endDate);
    }

    public long countFuturePendingDeliveries(LocalDate effectiveDate) {
        return deliveryDays.stream()
                .filter(day -> day.getDate().isAfter(effectiveDate))
                .filter(day -> day.getStatus() == DeliveryDayStatus.SCHEDULED
                        || day.getStatus() == DeliveryDayStatus.PAUSED)
                .count();
    }

    public void cancelFuturePendingDeliveries(LocalDate effectiveDate) {
        deliveryDays.stream()
                .filter(day -> day.getDate().isAfter(effectiveDate))
                .filter(day -> day.getStatus() == DeliveryDayStatus.SCHEDULED
                        || day.getStatus() == DeliveryDayStatus.PAUSED)
                .forEach(DeliveryDay::cancel);
    }

    private boolean isProcessed(DeliveryDay day) {
        return day.getStatus() == DeliveryDayStatus.CONSOLIDATED
                || day.getStatus() == DeliveryDayStatus.DELIVERED
                || day.getStatus() == DeliveryDayStatus.FAILED;
    }

    public Optional<DeliveryDay> findDayById(DeliveryDayId dayId) {
        return deliveryDays.stream()
                .filter(d -> d.getId().equals(dayId))
                .findFirst();
    }

    /**
     * Valida si quedaría al menos un día activo en el calendario si se excluye
     * el día indicado (por ej. si se marcara como NO_ENTREGA). Implementa la regla
     * INV-05.
     */
    public boolean hasActiveDaysAfterExcluding(DeliveryDayId dayId) {
        return deliveryDays.stream()
                .filter(d -> !d.getId().equals(dayId))
                .anyMatch(d -> d.getStatus() == DeliveryDayStatus.SCHEDULED
                        || d.getStatus() == DeliveryDayStatus.PAUSED
                        || d.getStatus() == DeliveryDayStatus.CONSOLIDATED
                        || d.getStatus() == DeliveryDayStatus.DELIVERED);
    }

    public List<DeliveryDay> getDaysForDate(LocalDate date) {
        return deliveryDays.stream()
                .filter(d -> d.getDate().equals(date))
                .toList();
    }

    public List<DeliveryDay> getDeliveryDays() {
        return Collections.unmodifiableList(deliveryDays);
    }

    public SubscriptionId getSubscriptionId() {
        return subscriptionId;
    }

    public ValidityPeriod getPeriod() {
        return period;
    }
}
