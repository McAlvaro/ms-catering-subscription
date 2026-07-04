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

    public JpaSubscriptionRepository(SpringDataSubscriptionRepository springRepository, SubscriptionMapper mapper) {
        this.springRepository = springRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<Subscription> findById(UUID id) {
        return springRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public void save(Subscription subscription) {
        SubscriptionJpaEntity jpaEntity = mapper.toJpaEntity(subscription);
        springRepository.save(jpaEntity);
    }

    @Override
    public int getNextContractSequenceOfYear() {
        int currentYear = Year.now().getValue();
        int currentCount = springRepository.countSubscriptionsByYear(currentYear);
        return currentCount + 1;
    }
}
