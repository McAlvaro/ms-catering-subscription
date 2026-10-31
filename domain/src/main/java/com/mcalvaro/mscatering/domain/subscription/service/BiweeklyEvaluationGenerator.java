package com.mcalvaro.mscatering.domain.subscription.service;

import java.time.LocalDate;
import java.util.List;

import com.mcalvaro.mscatering.domain.core.id.PatientId;
import com.mcalvaro.mscatering.domain.subscription.entity.BiweeklyEvaluation;
import com.mcalvaro.mscatering.domain.subscription.enums.PlanDuration;

/**
 * Domain Service for generating bi-weekly evaluations.
 */
public interface BiweeklyEvaluationGenerator {

    /**
     * Generates a list of evaluations for a given subscription period.
     *
     * @param patientId The patient ID.
     * @param startDate The start date of the subscription.
     * @param duration  The plan duration (15 or 30 days).
     * @return List of newly generated {@link BiweeklyEvaluation} instances.
     */
    List<BiweeklyEvaluation> generate(PatientId patientId, LocalDate startDate, PlanDuration duration);
}
