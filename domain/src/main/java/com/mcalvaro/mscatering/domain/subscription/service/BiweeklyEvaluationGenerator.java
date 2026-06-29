package com.mcalvaro.mscatering.domain.subscription.service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.mcalvaro.mscatering.domain.subscription.entity.BiweeklyEvaluation;
import com.mcalvaro.mscatering.domain.subscription.enums.PlanDuration;

/**
 * Domain Service for generating bi-weekly evaluations.
 * <p>
 * This logic is encapsulated in a Domain Service because calculating the exact
 * dates
 * requires knowledge of non-working days (holidays, weekends), which depends on
 * external
 * calendars and should not be part of the pure Aggregate Root logic.
 */
public interface BiweeklyEvaluationGenerator {

    /**
     * Generates a list of evaluations for a given subscription period.
     * The implementation will adjust the calculated dates to the next working day
     * if they fall on weekends or holidays.
     *
     * @param patientId The patient ID.
     * @param startDate The start date of the subscription.
     * @param duration  The plan duration (15 or 30 days).
     * @return List of newly generated {@link BiweeklyEvaluation} instances.
     */
    List<BiweeklyEvaluation> generate(UUID patientId, LocalDate startDate, PlanDuration duration);
}
