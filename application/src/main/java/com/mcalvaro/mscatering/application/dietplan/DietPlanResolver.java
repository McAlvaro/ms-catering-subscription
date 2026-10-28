package com.mcalvaro.mscatering.application.dietplan;

import com.mcalvaro.mscatering.domain.core.DomainException;
import com.mcalvaro.mscatering.domain.dietplan.DietPlanProjection;
import com.mcalvaro.mscatering.domain.dietplan.IDietPlanProjectionRepository;

import java.time.LocalDate;
import java.util.UUID;

/** Resolves immutable BC2 snapshots before a BC3 subscription is constructed. */
public class DietPlanResolver {
    private final IDietPlanProjectionRepository plans;

    public DietPlanResolver(IDietPlanProjectionRepository plans) {
        this.plans = plans;
    }

    public DietPlanProjection resolve(UUID planId, UUID patientId, LocalDate start, LocalDate end) {
        DietPlanProjection plan = plans.findById(planId)
                .orElseThrow(() -> failure("PLAN-001", "Diet plan was not found"));
        if (!plan.active()) {
            throw failure("PLAN-002", "Diet plan is inactive");
        }
        if (plan.ownerPatientId() != null && !plan.ownerPatientId().equals(patientId)) {
            throw failure("PLAN-003", "Personalized diet plan belongs to another patient");
        }
        LocalDate expectedEnd = plan.effectiveStart().plusDays(plan.durationDays() - 1L);
        if (!plan.effectiveStart().equals(start) || !expectedEnd.equals(end)) {
            throw failure("PLAN-004", "Subscription period does not match the diet-plan period");
        }
        return plan;
    }

    private DomainException failure(String code, String message) {
        return new DomainException(code, message);
    }
}
