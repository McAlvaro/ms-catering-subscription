package com.mcalvaro.mscatering.application.outbox.translation;

import com.mcalvaro.mscatering.domain.core.DomainEvent;
import com.mcalvaro.mscatering.integration.contracts.IntegrationEvent;

/**
 * Pure conversion of one persisted domain-event snapshot to its wire contract.
 */
public interface OutboxEventTranslator<E extends DomainEvent> {
    Class<E> sourceType();

    IntegrationEvent translate(E source);
}
