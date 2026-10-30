package com.mcalvaro.mscatering.infrastructure.persistence.subscription.repository;

import com.mcalvaro.mscatering.application.abstractions.DomainEventDispatcher;
import com.mcalvaro.mscatering.domain.subscription.Subscription;
import com.mcalvaro.mscatering.domain.subscription.enums.SubscriptionStatus;
import com.mcalvaro.mscatering.infrastructure.persistence.subscription.entity.SubscriptionJpaEntity;
import com.mcalvaro.mscatering.infrastructure.persistence.subscription.mapper.SubscriptionMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.ScopeNotActiveException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Year;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

class JpaSubscriptionRepositoryTest {

    private SpringDataSubscriptionRepository springRepository;
    private SubscriptionMapper mapper;
    private DomainEventDispatcher dispatcher;
    private ObjectProvider<DomainEventDispatcher> dispatcherProvider;
    private JpaSubscriptionRepository repository;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        springRepository = mock(SpringDataSubscriptionRepository.class);
        mapper = mock(SubscriptionMapper.class);
        dispatcher = mock(DomainEventDispatcher.class);
        dispatcherProvider = mock(ObjectProvider.class);
        when(dispatcherProvider.getIfAvailable()).thenReturn(dispatcher);

        repository = new JpaSubscriptionRepository(springRepository, mapper, dispatcherProvider);
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

        Subscription subscription = mock(Subscription.class);
        SubscriptionJpaEntity jpaEntity = mock(SubscriptionJpaEntity.class);
        when(mapper.toJpaEntity(subscription)).thenReturn(jpaEntity);

        repository.save(subscription);

        verify(springRepository).save(jpaEntity);
        verify(dispatcher).register(subscription);
    }

    @Test
    @DisplayName("save skips event dispatcher lookup when request scope is not active (Kafka/background threads)")
    void saveSkipsDispatcherWhenRequestScopeIsNotActive() {
        // No request attributes set on current thread
        Subscription subscription = mock(Subscription.class);
        SubscriptionJpaEntity jpaEntity = mock(SubscriptionJpaEntity.class);
        when(mapper.toJpaEntity(subscription)).thenReturn(jpaEntity);

        assertThatCode(() -> repository.save(subscription)).doesNotThrowAnyException();

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

        Subscription subscription = mock(Subscription.class);
        SubscriptionJpaEntity jpaEntity = mock(SubscriptionJpaEntity.class);
        when(mapper.toJpaEntity(subscription)).thenReturn(jpaEntity);

        assertThatCode(() -> repository.save(subscription)).doesNotThrowAnyException();
        verify(springRepository).save(jpaEntity);
    }

    @Test
    @DisplayName("save catches IllegalStateException silently if thrown during getIfAvailable")
    void saveCatchesIllegalStateExceptionSilently() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
        when(dispatcherProvider.getIfAvailable()).thenThrow(new IllegalStateException("No scope active"));

        Subscription subscription = mock(Subscription.class);
        SubscriptionJpaEntity jpaEntity = mock(SubscriptionJpaEntity.class);
        when(mapper.toJpaEntity(subscription)).thenReturn(jpaEntity);

        assertThatCode(() -> repository.save(subscription)).doesNotThrowAnyException();
        verify(springRepository).save(jpaEntity);
    }

    @Test
    @DisplayName("save handles null dispatcherProvider defensively without throwing")
    void saveHandlesNullDispatcherProviderDefensively() {
        JpaSubscriptionRepository repoWithNull = new JpaSubscriptionRepository(springRepository, mapper, null);
        Subscription subscription = mock(Subscription.class);
        SubscriptionJpaEntity jpaEntity = mock(SubscriptionJpaEntity.class);
        when(mapper.toJpaEntity(subscription)).thenReturn(jpaEntity);

        assertThatCode(() -> repoWithNull.save(subscription)).doesNotThrowAnyException();
        verify(springRepository).save(jpaEntity);
    }

    @Test
    @DisplayName("Delegates read methods to springRepository correctly")
    void delegatesReadMethods() {
        UUID id = UUID.randomUUID();
        Subscription subscription = mock(Subscription.class);
        SubscriptionJpaEntity jpaEntity = mock(SubscriptionJpaEntity.class);
        when(springRepository.findById(id)).thenReturn(Optional.of(jpaEntity));
        when(mapper.toDomain(jpaEntity)).thenReturn(subscription);

        assertThat(repository.findById(id)).contains(subscription);

        when(springRepository.countSubscriptionsByYear(Year.now().getValue())).thenReturn(5);
        assertThat(repository.getNextContractSequenceOfYear()).isEqualTo(6);

        when(springRepository.findByStatusIn(List.of(SubscriptionStatus.ACTIVE.name(), SubscriptionStatus.PAUSED.name())))
                .thenReturn(List.of(jpaEntity));
        assertThat(repository.findConsolidatableSubscriptions()).containsExactly(subscription);

        UUID patientId = UUID.randomUUID();
        when(springRepository.existsByPatientIdAndStatusIn(patientId, List.of(SubscriptionStatus.ACTIVE.name(), SubscriptionStatus.PAUSED.name())))
                .thenReturn(true);
        assertThat(repository.hasActiveOrPausedSubscription(patientId)).isTrue();
    }
}
