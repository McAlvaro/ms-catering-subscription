package com.mcalvaro.mscatering.application.subscription.GetSubscriptionDetails;

import an.awesome.pipelinr.Command;

import java.util.UUID;

/**
 * Query to retrieve the full details of a subscription.
 * Queries return DTOs directly, avoiding loading the heavy Aggregate Root.
 */
public record GetSubscriptionDetailsQuery(
        UUID subscriptionId) implements Command<SubscriptionDetailsDto> {
}
