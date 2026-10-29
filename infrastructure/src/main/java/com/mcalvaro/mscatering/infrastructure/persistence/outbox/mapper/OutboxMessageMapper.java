package com.mcalvaro.mscatering.infrastructure.persistence.outbox.mapper;

import com.mcalvaro.mscatering.application.outbox.OutboxMessage;
import com.mcalvaro.mscatering.infrastructure.persistence.outbox.entity.OutboxMessageJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class OutboxMessageMapper {

    public OutboxMessageJpaEntity toJpaEntity(OutboxMessage message) {
        if (message == null) {
            return null;
        }
        OutboxMessageJpaEntity entity = new OutboxMessageJpaEntity();
        entity.setId(message.id());
        entity.setType(message.type());
        entity.setContent(message.content());
        entity.setCreated(message.created());
        entity.setProcessed(message.processed());
        entity.setProcessedOn(message.processedOn());
        entity.setAttemptCount(message.attemptCount());
        entity.setLastError(message.lastError());
        entity.setAvailableAt(message.availableAt());
        entity.setLockedUntil(message.lockedUntil());
        entity.setLockedBy(message.lockedBy());
        entity.setCorrelationId(message.correlationId());
        entity.setTraceId(message.traceId());
        entity.setSpanId(message.spanId());
        return entity;
    }

    public OutboxMessage toApplication(OutboxMessageJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return new OutboxMessage(entity.getId(), entity.getType(), entity.getContent(), entity.getCreated(),
                entity.isProcessed(), entity.getProcessedOn(), entity.getAttemptCount(), entity.getLastError(),
                entity.getAvailableAt(), entity.getLockedUntil(), entity.getLockedBy(), entity.getCorrelationId(),
                entity.getTraceId(), entity.getSpanId());
    }
}
