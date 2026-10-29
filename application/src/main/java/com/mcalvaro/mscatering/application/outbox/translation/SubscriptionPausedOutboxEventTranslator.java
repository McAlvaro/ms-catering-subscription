package com.mcalvaro.mscatering.application.outbox.translation;

import com.mcalvaro.mscatering.domain.subscription.event.SubscriptionPaused;
import com.mcalvaro.mscatering.integration.contracts.SubscriptionPausedIntegrationEvent;

public final class SubscriptionPausedOutboxEventTranslator implements OutboxEventTranslator<SubscriptionPaused> {
    @Override
    public Class<SubscriptionPaused> sourceType() {
        return SubscriptionPaused.class;
    }

    @Override
    public SubscriptionPausedIntegrationEvent translate(SubscriptionPaused source) {
        if (source == null)
            throw new IntegrationEventTranslationException("SubscriptionPaused must not be null");
        return new SubscriptionPausedIntegrationEvent(source.eventId(), source.occurredAt(), source.correlationId(),
                new SubscriptionPausedIntegrationEvent.Data(source.subscriptionId(), source.patientId(),
                        source.pauseRange().startDate(),
                        source.pauseRange().endDate(), source.reason(), source.newEndDate(), source.requestedOn()));
    }
}
