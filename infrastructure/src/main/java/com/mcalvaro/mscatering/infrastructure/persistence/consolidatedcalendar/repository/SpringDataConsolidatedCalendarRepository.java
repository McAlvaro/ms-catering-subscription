package com.mcalvaro.mscatering.infrastructure.persistence.consolidatedcalendar.repository;

import com.mcalvaro.mscatering.infrastructure.persistence.consolidatedcalendar.entity.ConsolidatedCalendarJpaEntity;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataConsolidatedCalendarRepository extends JpaRepository<ConsolidatedCalendarJpaEntity, String> {

    Optional<ConsolidatedCalendarJpaEntity> findByDate(LocalDate date);
}
