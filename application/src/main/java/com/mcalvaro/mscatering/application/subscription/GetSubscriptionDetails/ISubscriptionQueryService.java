package com.mcalvaro.mscatering.application.subscription.GetSubscriptionDetails;

import java.util.Optional;
import java.util.UUID;

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
    Optional<SubscriptionDetailsDto> getSubscriptionDetails(UUID subscriptionId);
}
