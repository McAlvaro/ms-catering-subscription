package com.mcalvaro.mscatering.domain.subscription;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for the {@link Subscription} aggregate.
 * Implemented by the infrastructure layer.
 */
public interface ISubscriptionRepository {

    Optional<Subscription> findById(UUID id);

    void save(Subscription subscription);
}
