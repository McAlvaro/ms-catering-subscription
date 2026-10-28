package com.mcalvaro.mscatering.infrastructure.persistence.dietplan.repository;

import com.mcalvaro.mscatering.infrastructure.persistence.dietplan.entity.DietPlanProjectionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface SpringDataDietPlanProjectionRepository extends JpaRepository<DietPlanProjectionJpaEntity, UUID> { }
