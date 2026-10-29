package com.mcalvaro.mscatering.application.outbox;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Application port for durable outbox storage. */
public interface OutboxMessageRepository {

    void save(OutboxMessage message);

    Optional<OutboxMessage> findById(UUID id);

    List<OutboxMessage> findPendingAvailable(Instant now, int limit);
}
