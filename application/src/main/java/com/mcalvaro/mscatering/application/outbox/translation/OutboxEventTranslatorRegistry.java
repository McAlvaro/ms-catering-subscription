package com.mcalvaro.mscatering.application.outbox.translation;

import com.mcalvaro.mscatering.domain.core.DomainEvent;
import com.mcalvaro.mscatering.integration.contracts.IntegrationEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Closed, exact-class registry for approved outbox translations. */
public final class OutboxEventTranslatorRegistry {
    private final Map<Class<? extends DomainEvent>, OutboxEventTranslator<? extends DomainEvent>> translators;

    public OutboxEventTranslatorRegistry(List<? extends OutboxEventTranslator<? extends DomainEvent>> translators) {
        if (translators == null) {
            throw new IntegrationEventTranslationException("Translators must not be null");
        }
        Map<Class<? extends DomainEvent>, OutboxEventTranslator<? extends DomainEvent>> mutable = new HashMap<>();
        for (OutboxEventTranslator<? extends DomainEvent> translator : translators) {
            if (translator == null || translator.sourceType() == null) {
                throw new IntegrationEventTranslationException("Translator and source type must not be null");
            }
            if (mutable.putIfAbsent(translator.sourceType(), translator) != null) {
                throw new IntegrationEventTranslationException(
                        "Duplicate translator source type: " + translator.sourceType().getName());
            }
        }
        this.translators = Map.copyOf(mutable);
    }

    public IntegrationEvent translate(DomainEvent event) {
        if (event == null) {
            throw new IntegrationEventTranslationException("Domain event must not be null");
        }
        OutboxEventTranslator<? extends DomainEvent> translator = translators.get(event.getClass());
        if (translator == null) {
            throw new IntegrationEventTranslationException(
                    "Unsupported domain event type: " + event.getClass().getName());
        }
        return translateExact(translator, event);
    }

    @SuppressWarnings("unchecked")
    private static <E extends DomainEvent> IntegrationEvent translateExact(OutboxEventTranslator<E> translator,
            DomainEvent event) {
        return translator.translate((E) event);
    }
}
