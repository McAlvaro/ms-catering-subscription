package com.mcalvaro.mscatering.application.subscription.GetSubscriptionDetails;

import an.awesome.pipelinr.Command;
import com.mcalvaro.mscatering.domain.core.id.SubscriptionId;

/**
 * Query to retrieve the full details of a subscription.
 * Queries return DTOs directly, avoiding loading the heavy Aggregate Root.
 */
public record GetSubscriptionDetailsQuery(
        SubscriptionId subscriptionId) implements Command<SubscriptionDetailsDto> {

    public GetSubscriptionDetailsQuery(String subscriptionId) {
        this(SubscriptionId.of(subscriptionId));
    }
}
