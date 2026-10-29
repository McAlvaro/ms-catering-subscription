package com.mcalvaro.mscatering.infrastructure.events;

import an.awesome.pipelinr.Notification;
import com.mcalvaro.mscatering.application.abstractions.DomainEventNotification;
import com.mcalvaro.mscatering.application.outbox.OutboxMessage;
import com.mcalvaro.mscatering.application.outbox.OutboxMessageRepository;
import com.mcalvaro.mscatering.domain.core.CorrelatedDomainEvent;
import com.mcalvaro.mscatering.domain.core.DomainEvent;
import com.mcalvaro.mscatering.infrastructure.persistence.outbox.codec.EncodedDomainEvent;
import com.mcalvaro.mscatering.infrastructure.persistence.outbox.codec.JacksonDomainEventCodec;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class SaveDomainEventToOutboxHandler implements Notification.Handler<DomainEventNotification> {

    private final JacksonDomainEventCodec codec;
    private final OutboxMessageRepository repository;

    public SaveDomainEventToOutboxHandler(JacksonDomainEventCodec codec, OutboxMessageRepository repository) {
        this.codec = codec;
        this.repository = repository;
    }

    @Override
    public void handle(DomainEventNotification notification) {
        DomainEvent event = notification.domainEvent();
        if (!codec.supports(event)) {
            return;
        }
        if (!(event instanceof CorrelatedDomainEvent correlatedEvent)) {
            throw new IllegalStateException("Supported domain event must implement CorrelatedDomainEvent");
        }

        EncodedDomainEvent encoded = codec.encode(event);
        Instant capturedAt = Instant.now();
        repository.save(new OutboxMessage(event.eventId(), encoded.type(), encoded.content(), capturedAt,
                false, null, 0, null, capturedAt, null, null, correlatedEvent.correlationId(), null, null));
    }
}
