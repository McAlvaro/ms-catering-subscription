package com.mcalvaro.mscatering.domain.dietplan;

import java.util.Optional;
import java.util.UUID;

/** Lookup port reserved for subscription validation and future calendar resolution. */
public interface IDietPlanProjectionRepository {
    Optional<DietPlanProjection> findById(UUID planId);
}
