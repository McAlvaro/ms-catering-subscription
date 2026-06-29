package com.mcalvaro.mscatering.domain.subscription.entity;

import com.mcalvaro.mscatering.domain.core.Entity;
import com.mcalvaro.mscatering.domain.subscription.enums.DeliveryDayStatus;
import com.mcalvaro.mscatering.domain.subscription.vo.PauseRange;
import com.mcalvaro.mscatering.domain.subscription.vo.ValidityPeriod;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Entity that manages the full collection of delivery days for a subscription
 * period (CalendarioDeEntregas).
 */
public class DeliveryCalendar extends Entity {

    private final UUID subscriptionId;
    private final ValidityPeriod period;
    private final List<DeliveryDay> deliveryDays = new ArrayList<>();

    public DeliveryCalendar(UUID id, UUID subscriptionId, ValidityPeriod period) {
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

    public Optional<DeliveryDay> findDayById(UUID dayId) {
        return deliveryDays.stream()
                .filter(d -> d.getId().equals(dayId))
                .findFirst();
    }

    /**
     * Valida si quedaría al menos un día activo en el calendario si se excluye
     * el día indicado (por ej. si se marcara como NO_ENTREGA). Implementa la regla
     * INV-05.
     */
    public boolean hasActiveDaysAfterExcluding(UUID dayId) {
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

    public UUID getSubscriptionId() {
        return subscriptionId;
    }

    public ValidityPeriod getPeriod() {
        return period;
    }
}
