package com.mcalvaro.mscatering.domain.subscription.vo;

import com.mcalvaro.mscatering.domain.core.DomainException;

import java.time.LocalDate;

/**
 * Value Object representing a requested pause range (RangoDePausa).
 * <p>
 * Validates:
 * <ul>
 * <li>{@code startDate} is before or equal to {@code endDate}.</li>
 * </ul>
 */
public record PauseRange(LocalDate startDate, LocalDate endDate) {

    public PauseRange {
        if (startDate == null || endDate == null) {
            throw new DomainException("VO-013", "PauseRange: startDate and endDate must not be null.");
        }
        if (startDate.isAfter(endDate)) {
            throw new DomainException("VO-014", "PauseRange: startDate must be before or equal to endDate.");
        }
    }

    /** Returns the inclusive number of calendar days in this pause. */
    public int durationDays() {
        return (int) java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }
}
