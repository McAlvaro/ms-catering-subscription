package com.mcalvaro.mscatering.infrastructure.persistence.outbox.mapper;

import com.mcalvaro.mscatering.application.outbox.OutboxMessage;
import com.mcalvaro.mscatering.infrastructure.persistence.outbox.entity.OutboxMessageJpaEntity;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxMessageMapperTest {

    private final OutboxMessageMapper mapper = new OutboxMessageMapper();

    @Test
    void roundTripsEveryOutboxMessageFieldAtMicrosecondPrecision() {
        OutboxMessage source = new OutboxMessage(UUID.fromString("00000000-0000-0000-0000-000000000001"),
                "bc3.SubscriptionCreated", "{\"subscriptionId\":\"42\"}",
                Instant.parse("2026-10-28T10:15:30.123456789Z"), false, null, 3, "Transient failure",
                Instant.parse("2026-10-28T10:16:30.123456789Z"), Instant.parse("2026-10-28T10:17:30.123456789Z"),
                "outbox-worker-1", "correlation-42", "0123456789abcdef0123456789abcdef", "0123456789abcdef");

        OutboxMessageJpaEntity entity = mapper.toJpaEntity(source);
        OutboxMessage result = mapper.toApplication(entity);

        assertThat(result).isEqualTo(source);
        assertThat(entity.getId()).isEqualTo(source.id());
        assertThat(entity.getType()).isEqualTo(source.type());
        assertThat(entity.getContent()).isEqualTo(source.content());
        assertThat(entity.getCreated()).isEqualTo(source.created());
        assertThat(entity.isProcessed()).isEqualTo(source.processed());
        assertThat(entity.getProcessedOn()).isEqualTo(source.processedOn());
        assertThat(entity.getAttemptCount()).isEqualTo(source.attemptCount());
        assertThat(entity.getLastError()).isEqualTo(source.lastError());
        assertThat(entity.getAvailableAt()).isEqualTo(source.availableAt());
        assertThat(entity.getLockedUntil()).isEqualTo(source.lockedUntil());
        assertThat(entity.getLockedBy()).isEqualTo(source.lockedBy());
        assertThat(entity.getCorrelationId()).isEqualTo(source.correlationId());
        assertThat(entity.getTraceId()).isEqualTo(source.traceId());
        assertThat(entity.getSpanId()).isEqualTo(source.spanId());
    }
}
