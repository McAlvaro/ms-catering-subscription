package com.mcalvaro.mscatering.infrastructure.persistence.outbox.repository;

import com.mcalvaro.mscatering.application.outbox.OutboxMessage;
import com.mcalvaro.mscatering.application.outbox.OutboxMessageRepository;
import com.mcalvaro.mscatering.infrastructure.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sql.DataSource;
import java.sql.DatabaseMetaData;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class JpaOutboxMessageRepositoryTest extends BaseIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-10-28T12:00:00.123456Z");

    @Autowired
    private OutboxMessageRepository repository;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private SpringDataOutboxMessageRepository springDataRepository;

    @Test
    void persistsEveryFieldAndReadsOnlyAvailableUnlockedPendingMessagesInDeterministicOrder() {
        OutboxMessage first = pending("00000000-0000-0000-0000-000000000001", NOW.minusSeconds(30), NOW.minusSeconds(1),
                null, null, 1, "first error");
        OutboxMessage second = pending("00000000-0000-0000-0000-000000000002", NOW.minusSeconds(30),
                NOW.minusSeconds(1),
                null, null, 0, null);
        OutboxMessage expiredLock = pending("00000000-0000-0000-0000-000000000003", NOW.minusSeconds(20),
                NOW.minusSeconds(1),
                NOW.minusSeconds(1), "worker-expired", 0, null);
        OutboxMessage future = pending("00000000-0000-0000-0000-000000000004", NOW.minusSeconds(10), NOW.plusSeconds(1),
                null, null, 0, null);
        OutboxMessage locked = pending("00000000-0000-0000-0000-000000000005", NOW.minusSeconds(10),
                NOW.minusSeconds(1),
                NOW.plusSeconds(60), "worker-active", 0, null);
        OutboxMessage processed = new OutboxMessage(UUID.fromString("00000000-0000-0000-0000-000000000006"),
                "processed.type",
                "{\"processed\":true}", NOW.minusSeconds(10), true, NOW.minusSeconds(5), 2, null, NOW.minusSeconds(10),
                null, null, null, null, null);
        List.of(first, second, expiredLock, future, locked, processed).forEach(repository::save);

        List<OutboxMessage> candidates = repository.findPendingAvailable(NOW, 10);
        List<OutboxMessage> limited = repository.findPendingAvailable(NOW, 2);
        OutboxMessage roundTrip = repository.findById(first.id()).orElseThrow();

        assertThat(candidates).extracting(OutboxMessage::id)
                .containsExactly(first.id(), second.id(), expiredLock.id());
        assertThat(limited).extracting(OutboxMessage::id).containsExactly(first.id(), second.id());
        assertThat(roundTrip).isEqualTo(first);
        assertThat(roundTrip.created()).isEqualTo(Instant.parse("2026-10-28T11:59:30.123456Z"));
        assertThat(roundTrip.availableAt()).isEqualTo(Instant.parse("2026-10-28T11:59:59.123456Z"));
        assertThat(roundTrip.attemptCount()).isEqualTo(1);
        assertThat(roundTrip.lastError()).isEqualTo("first error");
        assertThat(roundTrip.lockedUntil()).isNull();
        assertThat(roundTrip.correlationId()).isEqualTo("correlation-1");
        assertThat(roundTrip.traceId()).isEqualTo("0123456789abcdef0123456789abcdef");
        assertThat(roundTrip.spanId()).isEqualTo("0123456789abcdef");
    }

    @Test
    void validatesPendingQueryArguments() {
        assertThatNullPointerException().isThrownBy(() -> repository.findPendingAvailable(null, 1));
        assertThatThrownBy(() -> repository.findPendingAvailable(NOW, 0))
                .hasMessageContaining("Limit must be greater than zero");
    }

    @Test
    void rejectsDuplicateIdsWithoutChangingTheOriginalMessageOrCreatingAnotherRow() {
        OutboxMessage original = pending("00000000-0000-0000-0000-000000000007", NOW, NOW, null, null, 0, null);
        OutboxMessage replacement = new OutboxMessage(original.id(), "replacement.type", "{\"replacement\":true}",
                NOW.plusSeconds(1), false, null, 2, "replacement error", NOW.plusSeconds(1), null, null,
                "replacement-correlation", null, null);

        repository.save(original);

        assertThatThrownBy(() -> repository.save(replacement)).isInstanceOf(RuntimeException.class);

        assertThat(repository.findById(original.id())).contains(original);
        assertThat(springDataRepository.count()).isEqualTo(1);
    }

    @Test
    void createsAllOutboxIndexes() throws Exception {
        List<String> indexNames = new ArrayList<>();
        try (var connection = dataSource.getConnection();
                var indexes = connection.getMetaData()
                        .getIndexInfo(null, null, "outbox_messages", false, false)) {
            while (indexes.next()) {
                String indexName = indexes.getString("INDEX_NAME");
                if (indexName != null) {
                    indexNames.add(indexName.toLowerCase());
                }
            }
        }

        assertThat(indexNames).contains("idx_outbox_pending_available", "idx_outbox_locks",
                "idx_outbox_processed_cleanup");
    }

    private OutboxMessage pending(String id, Instant created, Instant availableAt, Instant lockedUntil, String lockedBy,
            int attempts, String lastError) {
        return new OutboxMessage(UUID.fromString(id), "bc3.SubscriptionCreated", "{\"subscriptionId\":\"42\"}", created,
                false, null, attempts, lastError, availableAt, lockedUntil, lockedBy, "correlation-1",
                "0123456789abcdef0123456789abcdef", "0123456789abcdef");
    }
}
