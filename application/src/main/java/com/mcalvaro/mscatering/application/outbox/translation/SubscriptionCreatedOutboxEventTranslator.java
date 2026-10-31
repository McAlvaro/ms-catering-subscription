package com.mcalvaro.mscatering.application.outbox.translation;

import com.mcalvaro.mscatering.domain.dietplan.PlanType;
import com.mcalvaro.mscatering.domain.subscription.event.SubscriptionCreated;
import com.mcalvaro.mscatering.integration.contracts.SubscriptionCreatedIntegrationEvent;

/**
 * Maps the complete immutable creation snapshot without generating new values.
 */
public final class SubscriptionCreatedOutboxEventTranslator implements OutboxEventTranslator<SubscriptionCreated> {
    @Override
    public Class<SubscriptionCreated> sourceType() {
        return SubscriptionCreated.class;
    }

    @Override
    public SubscriptionCreatedIntegrationEvent translate(SubscriptionCreated source) {
        requireSource(source);
        return new SubscriptionCreatedIntegrationEvent(source.eventId(), source.occurredAt(), source.correlationId(),
                new SubscriptionCreatedIntegrationEvent.Data(source.subscriptionId().value(), source.patientId().value(),
                        source.dietPlanId().value(),
                        wirePlanType(source.planType()), source.durationDays(), source.periodStart(),
                        source.periodEnd(),
                        source.evaluationIds().size(), source.totalPrice(), null));
    }

    private static String wirePlanType(PlanType planType) {
        if (planType == null)
            throw new IntegrationEventTranslationException("SubscriptionCreated plan type must not be null");
        return switch (planType) {
            case BASE -> "BASE";
            case PERSONALIZED -> "PERSONALIZADO";
        };
    }

    private static void requireSource(SubscriptionCreated source) {
        if (source == null)
            throw new IntegrationEventTranslationException("SubscriptionCreated must not be null");
    }
}
