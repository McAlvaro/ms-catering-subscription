package com.mcalvaro.mscatering.application.integration;

import com.mcalvaro.mscatering.integration.contracts.IntegrationEvent;

/**
 * Outbound integration-event port. OUT-006 intentionally does not invoke it.
 */
public interface IntegrationEventPublisher {
    void publish(IntegrationEvent event);
}
