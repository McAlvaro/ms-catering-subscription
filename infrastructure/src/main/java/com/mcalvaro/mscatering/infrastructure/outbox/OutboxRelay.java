package com.mcalvaro.mscatering.infrastructure.outbox;

import org.springframework.scheduling.annotation.Scheduled;

/**
 * Scheduler facade kept separate from processing so dispatch has no long DB
 * transaction.
 */
public final class OutboxRelay {
    private final OutboxProcessor processor;
    private final OutboxRelayProperties properties;

    public OutboxRelay(OutboxProcessor processor, OutboxRelayProperties properties) {
        this.processor = processor;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${outbox.poll-interval:5s}")
    public void relay() {
        processor.processAvailable(properties.batchSize(), properties.leaseDuration());
    }
}
