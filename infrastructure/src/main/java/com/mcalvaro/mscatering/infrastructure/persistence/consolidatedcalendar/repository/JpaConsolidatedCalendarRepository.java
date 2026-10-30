package com.mcalvaro.mscatering.infrastructure.persistence.consolidatedcalendar.repository;

import com.mcalvaro.mscatering.application.abstractions.DomainEventDispatcher;
import com.mcalvaro.mscatering.domain.consolidatedcalendar.ConsolidatedCalendar;
import com.mcalvaro.mscatering.domain.consolidatedcalendar.IConsolidatedCalendarRepository;
import com.mcalvaro.mscatering.infrastructure.persistence.consolidatedcalendar.entity.ConsolidatedCalendarJpaEntity;
import com.mcalvaro.mscatering.infrastructure.persistence.consolidatedcalendar.mapper.ConsolidatedCalendarMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.ScopeNotActiveException;
import org.springframework.stereotype.Repository;
import org.springframework.web.context.request.RequestContextHolder;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaConsolidatedCalendarRepository implements IConsolidatedCalendarRepository {

    private final SpringDataConsolidatedCalendarRepository springRepository;
    private final ConsolidatedCalendarMapper mapper;
    private final ObjectProvider<DomainEventDispatcher> dispatcherProvider;

    @Autowired
    public JpaConsolidatedCalendarRepository(SpringDataConsolidatedCalendarRepository springRepository,
                                            ConsolidatedCalendarMapper mapper,
                                            ObjectProvider<DomainEventDispatcher> dispatcherProvider) {
        this.springRepository = springRepository;
        this.mapper = mapper;
        this.dispatcherProvider = dispatcherProvider;
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
        registerDomainEventsIfAvailable(calendar);
    }

    private void registerDomainEventsIfAvailable(ConsolidatedCalendar calendar) {
        if (dispatcherProvider == null) return;
        try {
            if (RequestContextHolder.getRequestAttributes() != null) {
                DomainEventDispatcher dispatcher = dispatcherProvider.getIfAvailable();
                if (dispatcher != null) {
                    dispatcher.register(calendar);
                }
            }
        } catch (ScopeNotActiveException | IllegalStateException ignored) {
            // Request scope not active on current thread (e.g. Kafka consumer)
        }
    }
}
