package com.mcalvaro.mscatering.infrastructure.persistence.subscription.repository;

import com.mcalvaro.mscatering.domain.subscription.ISubscriptionRepository;
import com.mcalvaro.mscatering.domain.subscription.Subscription;
import com.mcalvaro.mscatering.infrastructure.persistence.subscription.entity.SubscriptionJpaEntity;
import com.mcalvaro.mscatering.infrastructure.persistence.subscription.mapper.SubscriptionMapper;
import org.springframework.stereotype.Repository;

import java.time.Year;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaSubscriptionRepository implements ISubscriptionRepository {

    private final SpringDataSubscriptionRepository springRepository;
    private final SubscriptionMapper mapper;
    private final com.mcalvaro.mscatering.application.abstractions.DomainEventDispatcher dispatcher;

    public JpaSubscriptionRepository(SpringDataSubscriptionRepository springRepository, SubscriptionMapper mapper, com.mcalvaro.mscatering.application.abstractions.DomainEventDispatcher dispatcher) {
        this.springRepository = springRepository;
        this.mapper = mapper;
        this.dispatcher = dispatcher;
    }

    @Override
    public Optional<Subscription> findById(UUID id) {
        return springRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public void save(Subscription subscription) {
        SubscriptionJpaEntity jpaEntity = mapper.toJpaEntity(subscription);
        springRepository.save(jpaEntity);
        dispatcher.register(subscription);
    }

    @Override
    public int getNextContractSequenceOfYear() {
        int currentYear = Year.now().getValue();
        int currentCount = springRepository.countSubscriptionsByYear(currentYear);
        return currentCount + 1;
    }

    @Override
    public java.util.List<Subscription> findActiveSubscriptions() {
        return springRepository.findByStatus(com.mcalvaro.mscatering.domain.subscription.enums.SubscriptionStatus.ACTIVE.name())
                .stream()
                .map(mapper::toDomain)
                .collect(java.util.stream.Collectors.toList());
    }
}
