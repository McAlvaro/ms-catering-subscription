package com.mcalvaro.mscatering.infrastructure.outbox;

import com.mcalvaro.mscatering.application.integration.IntegrationEventPublisher;
import com.mcalvaro.mscatering.integration.contracts.IntegrationEvent;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Test transport that records actual publisher calls without a broker or
 * Mockito.
 */
final class RecordingIntegrationEventPublisher implements IntegrationEventPublisher {
    private final List<IntegrationEvent> published = new CopyOnWriteArrayList<>();
    private volatile RuntimeException failure;
    private volatile boolean transactionActiveAtInvocation;

    @Override
    public void publish(IntegrationEvent event) {
        transactionActiveAtInvocation = TransactionSynchronizationManager.isActualTransactionActive();
        if (failure != null) {
            throw failure;
        }
        published.add(event);
    }

    void failWith(RuntimeException failure) {
        this.failure = failure;
    }

    void reset() {
        published.clear();
        failure = null;
        transactionActiveAtInvocation = false;
    }

    List<IntegrationEvent> published() {
        return List.copyOf(published);
    }

    boolean transactionActiveAtInvocation() {
        return transactionActiveAtInvocation;
    }
}
