package com.mcalvaro.mscatering.infrastructure.persistence.consolidatedcalendar.repository;

import com.mcalvaro.mscatering.infrastructure.persistence.consolidatedcalendar.entity.ConsolidatedCalendarJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataConsolidatedCalendarRepository extends JpaRepository<ConsolidatedCalendarJpaEntity, UUID> {

    Optional<ConsolidatedCalendarJpaEntity> findByDate(LocalDate date);
}
