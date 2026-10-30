package com.mcalvaro.mscatering.infrastructure.persistence.consolidatedcalendar.repository;

import com.mcalvaro.mscatering.application.abstractions.DomainEventDispatcher;
import com.mcalvaro.mscatering.domain.consolidatedcalendar.ConsolidatedCalendar;
import com.mcalvaro.mscatering.infrastructure.persistence.consolidatedcalendar.entity.ConsolidatedCalendarJpaEntity;
import com.mcalvaro.mscatering.infrastructure.persistence.consolidatedcalendar.mapper.ConsolidatedCalendarMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.ScopeNotActiveException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

class JpaConsolidatedCalendarRepositoryTest {

    private SpringDataConsolidatedCalendarRepository springRepository;
    private ConsolidatedCalendarMapper mapper;
    private DomainEventDispatcher dispatcher;
    private ObjectProvider<DomainEventDispatcher> dispatcherProvider;
    private JpaConsolidatedCalendarRepository repository;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        springRepository = mock(SpringDataConsolidatedCalendarRepository.class);
        mapper = mock(ConsolidatedCalendarMapper.class);
        dispatcher = mock(DomainEventDispatcher.class);
        dispatcherProvider = mock(ObjectProvider.class);
        when(dispatcherProvider.getIfAvailable()).thenReturn(dispatcher);

        repository = new JpaConsolidatedCalendarRepository(springRepository, mapper, dispatcherProvider);
        RequestContextHolder.resetRequestAttributes();
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("save registers domain events when request scope is active")
    void saveRegistersDomainEventsWhenRequestScopeIsActive() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));

        ConsolidatedCalendar calendar = mock(ConsolidatedCalendar.class);
        ConsolidatedCalendarJpaEntity jpaEntity = mock(ConsolidatedCalendarJpaEntity.class);
        when(mapper.toJpaEntity(calendar)).thenReturn(jpaEntity);

        repository.save(calendar);

        verify(springRepository).save(jpaEntity);
        verify(dispatcher).register(calendar);
    }

    @Test
    @DisplayName("save skips event dispatcher lookup when request scope is not active (Kafka/background threads)")
    void saveSkipsDispatcherWhenRequestScopeIsNotActive() {
        // No request attributes set on current thread
        ConsolidatedCalendar calendar = mock(ConsolidatedCalendar.class);
        ConsolidatedCalendarJpaEntity jpaEntity = mock(ConsolidatedCalendarJpaEntity.class);
        when(mapper.toJpaEntity(calendar)).thenReturn(jpaEntity);

        assertThatCode(() -> repository.save(calendar)).doesNotThrowAnyException();

        verify(springRepository).save(jpaEntity);
        verify(dispatcherProvider, never()).getIfAvailable();
        verifyNoInteractions(dispatcher);
    }

    @Test
    @DisplayName("save catches ScopeNotActiveException silently if thrown during getIfAvailable")
    void saveCatchesScopeNotActiveExceptionSilently() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
        when(dispatcherProvider.getIfAvailable()).thenThrow(
                new ScopeNotActiveException("domainEventDispatcher", "request", new IllegalStateException("No request active")));

        ConsolidatedCalendar calendar = mock(ConsolidatedCalendar.class);
        ConsolidatedCalendarJpaEntity jpaEntity = mock(ConsolidatedCalendarJpaEntity.class);
        when(mapper.toJpaEntity(calendar)).thenReturn(jpaEntity);

        assertThatCode(() -> repository.save(calendar)).doesNotThrowAnyException();
        verify(springRepository).save(jpaEntity);
    }

    @Test
    @DisplayName("save catches IllegalStateException silently if thrown during getIfAvailable")
    void saveCatchesIllegalStateExceptionSilently() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
        when(dispatcherProvider.getIfAvailable()).thenThrow(new IllegalStateException("No scope active"));

        ConsolidatedCalendar calendar = mock(ConsolidatedCalendar.class);
        ConsolidatedCalendarJpaEntity jpaEntity = mock(ConsolidatedCalendarJpaEntity.class);
        when(mapper.toJpaEntity(calendar)).thenReturn(jpaEntity);

        assertThatCode(() -> repository.save(calendar)).doesNotThrowAnyException();
        verify(springRepository).save(jpaEntity);
    }

    @Test
    @DisplayName("save handles null dispatcherProvider defensively without throwing")
    void saveHandlesNullDispatcherProviderDefensively() {
        JpaConsolidatedCalendarRepository repoWithNull =
                new JpaConsolidatedCalendarRepository(springRepository, mapper, null);
        ConsolidatedCalendar calendar = mock(ConsolidatedCalendar.class);
        ConsolidatedCalendarJpaEntity jpaEntity = mock(ConsolidatedCalendarJpaEntity.class);
        when(mapper.toJpaEntity(calendar)).thenReturn(jpaEntity);

        assertThatCode(() -> repoWithNull.save(calendar)).doesNotThrowAnyException();
        verify(springRepository).save(jpaEntity);
    }

    @Test
    @DisplayName("Delegates read methods to springRepository correctly")
    void delegatesReadMethods() {
        UUID id = UUID.randomUUID();
        ConsolidatedCalendar calendar = mock(ConsolidatedCalendar.class);
        ConsolidatedCalendarJpaEntity jpaEntity = mock(ConsolidatedCalendarJpaEntity.class);
        when(springRepository.findById(id)).thenReturn(Optional.of(jpaEntity));
        when(mapper.toDomain(jpaEntity)).thenReturn(calendar);

        assertThat(repository.findById(id)).contains(calendar);

        LocalDate date = LocalDate.of(2026, 11, 1);
        when(springRepository.findByDate(date)).thenReturn(Optional.of(jpaEntity));
        assertThat(repository.findByDate(date)).contains(calendar);
    }
}
