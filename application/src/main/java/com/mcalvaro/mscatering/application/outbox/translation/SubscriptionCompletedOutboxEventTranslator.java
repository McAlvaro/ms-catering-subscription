package com.mcalvaro.mscatering.application.outbox.translation;

import com.mcalvaro.mscatering.domain.subscription.event.SubscriptionCompleted;
import com.mcalvaro.mscatering.integration.contracts.SubscriptionCancelledIntegrationEvent;

public final class SubscriptionCompletedOutboxEventTranslator implements OutboxEventTranslator<SubscriptionCompleted> {
    private static final String EXPIRATION_REASON = "Finalizacion del periodo contratado";

    @Override
    public Class<SubscriptionCompleted> sourceType() {
        return SubscriptionCompleted.class;
    }

    @Override
    public SubscriptionCancelledIntegrationEvent translate(SubscriptionCompleted source) {
        if (source == null)
            throw new IntegrationEventTranslationException("SubscriptionCompleted must not be null");
        return new SubscriptionCancelledIntegrationEvent(source.eventId(), source.occurredAt(), source.correlationId(),
                new SubscriptionCancelledIntegrationEvent.Data(source.subscriptionId(), source.patientId(),
                        source.dietPlanId(),
                        "VENCIMIENTO", EXPIRATION_REASON, source.effectiveDate(), source.cancelledPendingDeliveries()));
    }
}
