package com.mcalvaro.mscatering.infrastructure.persistence.subscription.repository;

import com.mcalvaro.mscatering.application.abstractions.DomainEventDispatcher;
import com.mcalvaro.mscatering.domain.core.id.PatientId;
import com.mcalvaro.mscatering.domain.core.id.SubscriptionId;
import com.mcalvaro.mscatering.domain.subscription.ISubscriptionRepository;
import com.mcalvaro.mscatering.domain.subscription.Subscription;
import com.mcalvaro.mscatering.domain.subscription.enums.SubscriptionStatus;
import com.mcalvaro.mscatering.infrastructure.persistence.subscription.entity.SubscriptionJpaEntity;
import com.mcalvaro.mscatering.infrastructure.persistence.subscription.mapper.SubscriptionMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.ScopeNotActiveException;
import org.springframework.stereotype.Repository;
import org.springframework.web.context.request.RequestContextHolder;

import java.time.Year;
import java.util.List;
import java.util.Optional;

@Repository
public class JpaSubscriptionRepository implements ISubscriptionRepository {

    private final SpringDataSubscriptionRepository springRepository;
    private final SubscriptionMapper mapper;
    private final ObjectProvider<DomainEventDispatcher> dispatcherProvider;

    @Autowired
    public JpaSubscriptionRepository(SpringDataSubscriptionRepository springRepository,
                                    SubscriptionMapper mapper,
                                    ObjectProvider<DomainEventDispatcher> dispatcherProvider) {
        this.springRepository = springRepository;
        this.mapper = mapper;
        this.dispatcherProvider = dispatcherProvider;
    }

    @Override
    public Optional<Subscription> findById(SubscriptionId id) {
        return springRepository.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public void save(Subscription subscription) {
        SubscriptionJpaEntity jpaEntity = mapper.toJpaEntity(subscription);
        springRepository.save(jpaEntity);
        registerDomainEventsIfAvailable(subscription);
    }

    private void registerDomainEventsIfAvailable(Subscription subscription) {
        if (dispatcherProvider == null) return;
        try {
            if (RequestContextHolder.getRequestAttributes() != null) {
                DomainEventDispatcher dispatcher = dispatcherProvider.getIfAvailable();
                if (dispatcher != null) {
                    dispatcher.register(subscription);
                }
            }
        } catch (ScopeNotActiveException | IllegalStateException ignored) {
            // Request scope not active on current thread (e.g. Kafka consumer)
        }
    }

    @Override
    public int getNextContractSequenceOfYear() {
        int currentYear = Year.now().getValue();
        int currentCount = springRepository.countSubscriptionsByYear(currentYear);
        return currentCount + 1;
    }

    @Override
    public java.util.List<Subscription> findConsolidatableSubscriptions() {
        return springRepository
                .findByStatusIn(List.of(SubscriptionStatus.ACTIVE.name(), SubscriptionStatus.PAUSED.name()))
                .stream()
                .map(mapper::toDomain)
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public boolean hasActiveOrPausedSubscription(PatientId patientId) {
        return springRepository.existsByPatientIdAndStatusIn(
                patientId.value(),
                List.of(
                        SubscriptionStatus.ACTIVE.name(),
                        SubscriptionStatus.PAUSED.name()));
    }
}
