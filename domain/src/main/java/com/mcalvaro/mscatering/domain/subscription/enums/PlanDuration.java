package com.mcalvaro.mscatering.domain.subscription.enums;

import java.util.Arrays;

import com.mcalvaro.mscatering.domain.core.DomainException;

/**
 * Enum representing the allowed subscription plan durations.
 * <p>
 * The business defines exactly two plan types:
 * <ul>
 * <li>{@link #BIWEEKLY} — 15-day plan (quincenal): 1 biweekly evaluation.</li>
 * <li>{@link #MONTHLY} — 30-day plan (mensual): 2 biweekly evaluations.</li>
 * </ul>
 * <p>
 * Encoding the allowed durations as an enum eliminates magic numbers
 * and makes the domain rule explicit and self-documenting.
 */
public enum PlanDuration {

    BIWEEKLY(15),
    MONTHLY(30);

    private final int days;

    PlanDuration(int days) {
        this.days = days;
    }

    public int getDays() {
        return days;
    }

    /**
     * Resolves a {@link PlanDuration} from a raw number of days.
     * Throws {@link DomainException} if the duration is not a valid plan.
     */
    public static PlanDuration fromDays(int days) {
        return Arrays.stream(values())
                .filter(d -> d.days == days)
                .findFirst()
                .orElseThrow(() -> new DomainException(
                        "SUB-005", "La duración del plan debe ser de 15 o 30 días, se obtuvo: " + days));
    }
}
