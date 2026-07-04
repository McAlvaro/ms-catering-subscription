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

    /**
     * Returns the next available contract sequence number for the current year.
     * Used by the application layer to generate unique
     * {@link com.mcalvaro.mscatering.domain.subscription.vo.ContractCode}
     * values in the format {@code NTC-YYYY-NNNN}.
     *
     * @return The next sequential number (e.g., if 45 contracts exist this year,
     *         returns 46).
     */
    int getNextContractSequenceOfYear();

    /**
     * Finds all active subscriptions in the system.
     * Used by the consolidation process.
     */
    java.util.List<Subscription> findActiveSubscriptions();
}
