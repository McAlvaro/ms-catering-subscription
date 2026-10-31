package com.mcalvaro.mscatering.domain.subscription;

import com.mcalvaro.mscatering.domain.core.id.PatientId;
import com.mcalvaro.mscatering.domain.core.id.SubscriptionId;

import java.util.Optional;

/**
 * Repository interface for the {@link Subscription} aggregate.
 * Implemented by the infrastructure layer.
 */
public interface ISubscriptionRepository {

    Optional<Subscription> findById(SubscriptionId id);

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
     * Finds subscriptions whose scheduled delivery days can be consolidated.
     * ACTIVE and PAUSED subscriptions are both included; paused delivery days are
     * excluded by their own status.
     */
    java.util.List<Subscription> findConsolidatableSubscriptions();

    /**
     * Checks if a patient has an ACTIVE or PAUSED subscription.
     */
    boolean hasActiveOrPausedSubscription(PatientId patientId);
}
