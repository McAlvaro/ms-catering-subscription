package com.mcalvaro.mscatering.application.outbox.translation;

import com.mcalvaro.mscatering.domain.subscription.event.SubscriptionReactivated;
import com.mcalvaro.mscatering.integration.contracts.SubscriptionReactivatedIntegrationEvent;

public final class SubscriptionReactivatedOutboxEventTranslator
        implements OutboxEventTranslator<SubscriptionReactivated> {
    @Override
    public Class<SubscriptionReactivated> sourceType() {
        return SubscriptionReactivated.class;
    }

    @Override
    public SubscriptionReactivatedIntegrationEvent translate(SubscriptionReactivated source) {
        if (source == null)
            throw new IntegrationEventTranslationException("SubscriptionReactivated must not be null");
        return new SubscriptionReactivatedIntegrationEvent(source.eventId(), source.occurredAt(),
                source.correlationId(),
                new SubscriptionReactivatedIntegrationEvent.Data(source.subscriptionId(), source.patientId(),
                        source.reactivatedOn(), source.effectiveEndDate()));
    }
}
