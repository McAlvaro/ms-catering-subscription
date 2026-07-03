package com.mcalvaro.mscatering.application.subscription.GetSubscriptionDetails;

import an.awesome.pipelinr.Command;

/**
 * Handler for {@link GetSubscriptionDetailsQuery}.
 * <p>
 * Uses the {@link ISubscriptionQueryService} port to fetch the read model
 * directly.
 */
public class GetSubscriptionDetailsQueryHandler
        implements Command.Handler<GetSubscriptionDetailsQuery, SubscriptionDetailsDto> {

    private final ISubscriptionQueryService queryService;

    public GetSubscriptionDetailsQueryHandler(ISubscriptionQueryService queryService) {
        this.queryService = queryService;
    }

    @Override
    public SubscriptionDetailsDto handle(GetSubscriptionDetailsQuery query) {
        return queryService.getSubscriptionDetails(query.subscriptionId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Subscription not found: " + query.subscriptionId()));
    }
}
