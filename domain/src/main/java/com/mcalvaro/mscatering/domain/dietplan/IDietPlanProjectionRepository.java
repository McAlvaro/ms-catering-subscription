package com.mcalvaro.mscatering.domain.dietplan;

import com.mcalvaro.mscatering.domain.core.id.DietPlanId;

import java.util.Optional;

/** Lookup port reserved for subscription validation and future calendar resolution. */
public interface IDietPlanProjectionRepository {
    Optional<DietPlanProjection> findById(DietPlanId planId);
}
