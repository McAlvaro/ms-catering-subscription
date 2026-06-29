package com.mcalvaro.mscatering.domain.subscription.entity;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import com.mcalvaro.mscatering.domain.core.DomainException;
import com.mcalvaro.mscatering.domain.core.Entity;
import com.mcalvaro.mscatering.domain.subscription.SubscriptionErrors;
import com.mcalvaro.mscatering.domain.subscription.enums.DeliveryDayStatus;
import com.mcalvaro.mscatering.domain.subscription.vo.DeliveryAddress;
import com.mcalvaro.mscatering.domain.subscription.vo.TimeWindow;

/**
 * Entity representing a single physical delivery on a given date
 * (DiaDeEntrega).
 * <p>
 * The patient may modify address, time window and instructions up to
 * 48 hours before the scheduled delivery date.
 */
public class DeliveryDay extends Entity {

    private final LocalDate date;
    private DeliveryDayStatus status;
    private DeliveryAddress address;
    private TimeWindow timeWindow;
    private String instructions;
    private Instant consolidatedAt;
    private Instant deliveredAt;
    private String failureReason;

    public DeliveryDay(UUID id, LocalDate date, DeliveryAddress address,
            TimeWindow timeWindow, String instructions) {
        super(id);
        this.date = date;
        this.address = address;
        this.timeWindow = timeWindow;
        this.instructions = instructions;
        this.status = DeliveryDayStatus.SCHEDULED;
    }

    /**
     * Modifies the delivery details. Only allowed if the delivery is at least
     * 48 hours away and the current status is {@code SCHEDULED}.
     */
    public void modify(DeliveryAddress newAddress, TimeWindow newTimeWindow, String newInstructions) {
        if (status == DeliveryDayStatus.CONSOLIDATED || status == DeliveryDayStatus.DELIVERED
                || status == DeliveryDayStatus.FAILED) {
            throw SubscriptionErrors.deliveryDayBlocked();
        }
        if (status != DeliveryDayStatus.SCHEDULED) {
            throw new DomainException("SUB-002",
                    "Delivery day cannot be modified: current status is " + status);
        }
        LocalDateTime deliveryMidnight = date.atStartOfDay();
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        if (!now.plusHours(48).isBefore(deliveryMidnight)) {
            throw SubscriptionErrors.cannotModifyDeliveryWithin48Hours();
        }
        this.address = newAddress;
        this.timeWindow = newTimeWindow;
        this.instructions = newInstructions;
    }

    public void markAsDelivered() {
        this.status = DeliveryDayStatus.DELIVERED;
        this.deliveredAt = Instant.now();
    }

    public void markAsFailed(String reason) {
        this.status = DeliveryDayStatus.FAILED;
        this.failureReason = reason;
    }

    public void markAsNoDelivery() {
        this.status = DeliveryDayStatus.NOT_DELIVERED;
    }

    public void pause() {
        this.status = DeliveryDayStatus.PAUSED;
    }

    public void reactivate() {
        this.status = DeliveryDayStatus.SCHEDULED;
    }

    public void cancel() {
        this.status = DeliveryDayStatus.CANCELLED;
    }

    public void markAsConsolidated() {
        this.status = DeliveryDayStatus.CONSOLIDATED;
        this.consolidatedAt = Instant.now();
    }

    public LocalDate getDate() {
        return date;
    }

    public DeliveryDayStatus getStatus() {
        return status;
    }

    public DeliveryAddress getAddress() {
        return address;
    }

    public TimeWindow getTimeWindow() {
        return timeWindow;
    }

    public String getInstructions() {
        return instructions;
    }

    public Instant getConsolidatedAt() {
        return consolidatedAt;
    }

    public Instant getDeliveredAt() {
        return deliveredAt;
    }

    public String getFailureReason() {
        return failureReason;
    }
}
