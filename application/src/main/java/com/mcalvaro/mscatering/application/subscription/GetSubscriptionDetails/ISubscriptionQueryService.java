package com.mcalvaro.mscatering.application.subscription.GetSubscriptionDetails;

import com.mcalvaro.mscatering.domain.core.id.SubscriptionId;

import java.util.Optional;

/**
 * Port (abstraction) for reading Subscription data (CQRS Read side).
 * <p>
 * This interface bypasses the heavy Aggregate Root (Subscription) and directly
 * queries the database to build lightweight DTOs.
 * Implemented by the Infrastructure layer.
 */
public interface ISubscriptionQueryService {

    /**
     * Retrieves the full details of a subscription by its ID.
     */
    Optional<SubscriptionDetailsDto> getSubscriptionDetails(SubscriptionId subscriptionId);

    default Optional<SubscriptionDetailsDto> getSubscriptionDetails(String subscriptionId) {
        return getSubscriptionDetails(SubscriptionId.of(subscriptionId));
    }
}
