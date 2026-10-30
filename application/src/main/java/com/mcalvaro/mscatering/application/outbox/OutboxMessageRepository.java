package com.mcalvaro.mscatering.application.outbox;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Application port for durable outbox storage. */
public interface OutboxMessageRepository {

    /**
     * Inserts a new message. Implementations must reject duplicate message IDs
     * rather than updating an existing row.
     */
    void save(OutboxMessage message);

    Optional<OutboxMessage> findById(UUID id);

    List<OutboxMessage> findPendingAvailable(Instant now, int limit);

    Optional<OutboxMessage> tryClaim(UUID id, Instant now, Instant leaseUntil, String lockToken);

    boolean markProcessedIfOwned(UUID id, String lockToken, Instant processedOn);

    boolean rescheduleAfterFailureIfOwned(UUID id, String lockToken, Instant availableAt, String lastError);
}
