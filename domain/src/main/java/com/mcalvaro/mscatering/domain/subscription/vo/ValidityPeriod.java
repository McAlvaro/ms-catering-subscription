package com.mcalvaro.mscatering.domain.subscription.vo;

import com.mcalvaro.mscatering.domain.core.DomainException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Value Object representing the active contract period (PeriodoDeVigencia).
 * <p>
 * {@code durationDays} is intentionally a derived method (not a record field)
 * to avoid redundant state in {@code equals}/{@code hashCode}.
 */
public record ValidityPeriod(LocalDate startDate, LocalDate endDate) {

    public ValidityPeriod {
        if (startDate == null || endDate == null) {
            throw new DomainException("VO-007", "ValidityPeriod: startDate and endDate must not be null.");
        }
        if (!startDate.isBefore(endDate)) {
            throw new DomainException("VO-008", "ValidityPeriod: startDate must be strictly before endDate.");
        }
    }

    /** Returns the number of days in the period (derived, not stored). */
    public int durationDays() {
        return (int) ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }

    /**
     * Returns {@code true} if {@code date} falls within [startDate, endDate]
     * inclusive.
     */
    public boolean contains(LocalDate date) {
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    /** Returns all days in the period, inclusive of both ends. */
    public List<LocalDate> allDays() {
        return startDate.datesUntil(endDate.plusDays(1)).toList();
    }
}
