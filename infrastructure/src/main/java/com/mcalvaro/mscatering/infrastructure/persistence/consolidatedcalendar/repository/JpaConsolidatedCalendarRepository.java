package com.mcalvaro.mscatering.infrastructure.persistence.consolidatedcalendar.repository;

import com.mcalvaro.mscatering.domain.consolidatedcalendar.ConsolidatedCalendar;
import com.mcalvaro.mscatering.domain.consolidatedcalendar.IConsolidatedCalendarRepository;
import com.mcalvaro.mscatering.infrastructure.persistence.consolidatedcalendar.entity.ConsolidatedCalendarJpaEntity;
import com.mcalvaro.mscatering.infrastructure.persistence.consolidatedcalendar.mapper.ConsolidatedCalendarMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaConsolidatedCalendarRepository implements IConsolidatedCalendarRepository {

    private final SpringDataConsolidatedCalendarRepository springRepository;
    private final ConsolidatedCalendarMapper mapper;

    public JpaConsolidatedCalendarRepository(SpringDataConsolidatedCalendarRepository springRepository, ConsolidatedCalendarMapper mapper) {
        this.springRepository = springRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<ConsolidatedCalendar> findById(UUID id) {
        return springRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<ConsolidatedCalendar> findByDate(LocalDate date) {
        return springRepository.findByDate(date).map(mapper::toDomain);
    }

    @Override
    public void save(ConsolidatedCalendar calendar) {
        ConsolidatedCalendarJpaEntity jpaEntity = mapper.toJpaEntity(calendar);
        springRepository.save(jpaEntity);
    }
}
