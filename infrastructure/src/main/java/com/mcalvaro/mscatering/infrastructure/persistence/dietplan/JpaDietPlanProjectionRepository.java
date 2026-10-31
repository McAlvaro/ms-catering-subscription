package com.mcalvaro.mscatering.infrastructure.persistence.dietplan;

import com.mcalvaro.mscatering.domain.core.id.DietPlanId;
import com.mcalvaro.mscatering.domain.dietplan.DietPlanProjection;
import com.mcalvaro.mscatering.domain.dietplan.IDietPlanProjectionRepository;
import com.mcalvaro.mscatering.infrastructure.persistence.dietplan.repository.SpringDataDietPlanProjectionRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public class JpaDietPlanProjectionRepository implements IDietPlanProjectionRepository {
    private final SpringDataDietPlanProjectionRepository repository;
    private final DietPlanProjectionMapper mapper;
    public JpaDietPlanProjectionRepository(SpringDataDietPlanProjectionRepository repository, DietPlanProjectionMapper mapper) { this.repository = repository; this.mapper = mapper; }
    @Override public Optional<DietPlanProjection> findById(DietPlanId planId) { return repository.findById(planId.value()).map(mapper::toDomain); }
}
