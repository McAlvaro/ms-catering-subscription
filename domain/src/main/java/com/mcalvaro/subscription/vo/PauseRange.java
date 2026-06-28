package com.mcalvaro.subscription.vo;

import com.mcalvaro.core.DomainException;

import java.time.LocalDate;

/**
 * Value Object representing a requested pause range (RangoDePausa).
 * <p>
 * Validates:
 * <ul>
 * <li>{@code startDate} is at least 48 hours (2 days) in the future.</li>
 * <li>{@code startDate} is strictly before {@code endDate}.</li>
 * </ul>
 */
public record PauseRange(LocalDate startDate, LocalDate endDate) {

    public PauseRange {
        if (startDate == null || endDate == null) {
            throw new DomainException("VO-013", "PauseRange: startDate and endDate must not be null.");
        }
        if (!startDate.isAfter(LocalDate.now().plusDays(1))) {
            throw new DomainException("SUB-001",
                    "Pause must be requested at least 48 hours in advance.");
        }
        if (!startDate.isBefore(endDate)) {
            throw new DomainException("VO-014", "PauseRange: startDate must be strictly before endDate.");
        }
    }
}
