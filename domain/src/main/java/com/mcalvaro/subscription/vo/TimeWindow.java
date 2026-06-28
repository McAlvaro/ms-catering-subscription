package com.mcalvaro.subscription.vo;

import com.mcalvaro.core.DomainException;

import java.time.LocalTime;

/**
 * Value Object that defines a delivery time slot.
 * Validates that {@code startTime} is strictly before {@code endTime}.
 */
public record TimeWindow(LocalTime startTime, LocalTime endTime) {

    public TimeWindow {
        if (startTime == null || endTime == null) {
            throw new DomainException("VO-001", "TimeWindow: startTime and endTime must not be null.");
        }
        if (!startTime.isBefore(endTime)) {
            throw new DomainException("VO-002", "TimeWindow: startTime must be before endTime.");
        }
    }
}
