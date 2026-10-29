package com.mcalvaro.mscatering.application.outbox.translation;

import com.mcalvaro.mscatering.domain.subscription.event.SubscriptionCancelled;
import com.mcalvaro.mscatering.integration.contracts.SubscriptionCancelledIntegrationEvent;

public final class SubscriptionCancelledOutboxEventTranslator implements OutboxEventTranslator<SubscriptionCancelled> {
    @Override
    public Class<SubscriptionCancelled> sourceType() {
        return SubscriptionCancelled.class;
    }

    @Override
    public SubscriptionCancelledIntegrationEvent translate(SubscriptionCancelled source) {
        if (source == null)
            throw new IntegrationEventTranslationException("SubscriptionCancelled must not be null");
        return new SubscriptionCancelledIntegrationEvent(source.eventId(), source.occurredAt(), source.correlationId(),
                new SubscriptionCancelledIntegrationEvent.Data(source.subscriptionId(), source.patientId(),
                        source.dietPlanId(),
                        "MANUAL", source.reason(), source.effectiveDate(), source.cancelledPendingDeliveries()));
    }
}
