package com.mcalvaro.mscatering.infrastructure.persistence.outbox.repository;

import com.mcalvaro.mscatering.application.outbox.OutboxMessage;
import com.mcalvaro.mscatering.application.outbox.OutboxMessageRepository;
import com.mcalvaro.mscatering.infrastructure.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.sql.DatabaseMetaData;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

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

    @Autowired
    private PlatformTransactionManager transactionManager;

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

    @Test
    void claimsMarksAndReschedulesOnlyForTheCurrentLeaseOwner() {
        OutboxMessage message = pending("00000000-0000-0000-0000-000000000008", NOW.minusSeconds(10),
                NOW.minusSeconds(1), null, null, 0, null);
        persistCommitted(message);

        assertThat(repository.tryClaim(message.id(), NOW, NOW.plusSeconds(60), "first-token"))
                .isPresent()
                .get()
                .extracting(OutboxMessage::lockedBy, OutboxMessage::lockedUntil)
                .containsExactly("first-token", NOW.plusSeconds(60));
        assertThat(repository.tryClaim(message.id(), NOW, NOW.plusSeconds(60), "second-token")).isEmpty();
        assertThat(repository.markProcessedIfOwned(message.id(), "second-token", NOW.plusSeconds(1))).isFalse();
        assertThat(repository.rescheduleAfterFailureIfOwned(message.id(), "first-token", NOW.plusSeconds(5), "boom"))
                .isTrue();

        OutboxMessage retried = repository.findById(message.id()).orElseThrow();
        assertThat(retried).extracting(OutboxMessage::attemptCount, OutboxMessage::lastError,
                OutboxMessage::availableAt, OutboxMessage::lockedBy, OutboxMessage::lockedUntil)
                .containsExactly(1, "boom", NOW.plusSeconds(5), null, null);
        assertThat(repository.tryClaim(message.id(), NOW.plusSeconds(5), NOW.plusSeconds(65), "second-token"))
                .isPresent();
        assertThat(repository.markProcessedIfOwned(message.id(), "first-token", NOW.plusSeconds(6))).isFalse();
        assertThat(repository.markProcessedIfOwned(message.id(), "second-token", NOW.plusSeconds(6))).isTrue();

        OutboxMessage processed = findCommitted(message.id());
        assertThat(processed).extracting(OutboxMessage::processed, OutboxMessage::processedOn,
                OutboxMessage::lockedBy, OutboxMessage::lockedUntil, OutboxMessage::lastError)
                .containsExactly(true, NOW.plusSeconds(6), null, null, null);
    }

    @Test
    void allowsOneConcurrentClaimAndRecoversAnExpiredLease() throws Exception {
        OutboxMessage message = pending("00000000-0000-0000-0000-000000000009", NOW.minusSeconds(10),
                NOW.minusSeconds(1), null, null, 0, null);
        persistCommitted(message);

        var executor = Executors.newFixedThreadPool(2);
        try {
            List<Callable<Boolean>> claims = List.of(
                    () -> repository.tryClaim(message.id(), NOW, NOW.plusSeconds(10), "worker-one").isPresent(),
                    () -> repository.tryClaim(message.id(), NOW, NOW.plusSeconds(10), "worker-two").isPresent());
            assertThat(executor.invokeAll(claims).stream().filter(future -> {
                try {
                    return future.get();
                } catch (Exception exception) {
                    throw new AssertionError(exception);
                }
            })).hasSize(1);
        } finally {
            executor.shutdownNow();
        }

        assertThat(repository.tryClaim(message.id(), NOW.plusSeconds(11), NOW.plusSeconds(20), "recovered"))
                .isPresent();
    }

    @Test
    void validatesEveryLeaseLifecycleArgumentAndRequiresAnIncreasingLease() {
        UUID id = UUID.randomUUID();
        assertValidation(() -> repository.tryClaim(null, NOW, NOW.plusSeconds(1), "token"), NullPointerException.class);
        assertValidation(() -> repository.tryClaim(id, null, NOW.plusSeconds(1), "token"), NullPointerException.class);
        assertValidation(() -> repository.tryClaim(id, NOW, null, "token"), NullPointerException.class);
        assertValidation(() -> repository.tryClaim(id, NOW, NOW.plusSeconds(1), null), NullPointerException.class);
        assertValidation(() -> repository.tryClaim(id, NOW, NOW.plusSeconds(1), " "), IllegalArgumentException.class);
        assertValidation(() -> repository.tryClaim(id, NOW, NOW, "token"), IllegalArgumentException.class,
                "Lease expiry must be after current time");
        assertValidation(() -> repository.tryClaim(id, NOW, NOW.minusSeconds(1), "token"), IllegalArgumentException.class,
                "Lease expiry must be after current time");

        assertValidation(() -> repository.markProcessedIfOwned(null, "token", NOW), NullPointerException.class);
        assertValidation(() -> repository.markProcessedIfOwned(id, null, NOW), NullPointerException.class);
        assertValidation(() -> repository.markProcessedIfOwned(id, " ", NOW), IllegalArgumentException.class);
        assertValidation(() -> repository.markProcessedIfOwned(id, "token", null), NullPointerException.class);

        assertValidation(() -> repository.rescheduleAfterFailureIfOwned(null, "token", NOW, "error"),
                NullPointerException.class);
        assertValidation(() -> repository.rescheduleAfterFailureIfOwned(id, null, NOW, "error"),
                NullPointerException.class);
        assertValidation(() -> repository.rescheduleAfterFailureIfOwned(id, " ", NOW, "error"),
                IllegalArgumentException.class);
        assertValidation(() -> repository.rescheduleAfterFailureIfOwned(id, "token", null, "error"),
                NullPointerException.class);
        assertValidation(() -> repository.rescheduleAfterFailureIfOwned(id, "token", NOW, null),
                NullPointerException.class);
        assertValidation(() -> repository.rescheduleAfterFailureIfOwned(id, "token", NOW, " \t"),
                IllegalArgumentException.class);
    }

    @Test
    void rejectsProcessedFutureAndActivelyLockedMessagesForClaim() {
        OutboxMessage processed = new OutboxMessage(UUID.randomUUID(), "processed.type", "{}", NOW.minusSeconds(2), true,
                NOW.minusSeconds(1), 0, null, NOW.minusSeconds(2), null, null, null, null, null);
        OutboxMessage future = pending(UUID.randomUUID().toString(), NOW.minusSeconds(1), NOW.plusSeconds(1), null, null, 0,
                null);
        OutboxMessage activeLock = pending(UUID.randomUUID().toString(), NOW.minusSeconds(1), NOW.minusSeconds(1),
                NOW.plusSeconds(1), "another-worker", 0, null);
        List.of(processed, future, activeLock).forEach(this::persistCommitted);

        assertThat(repository.tryClaim(processed.id(), NOW, NOW.plusSeconds(10), "worker")).isEmpty();
        assertThat(repository.tryClaim(future.id(), NOW, NOW.plusSeconds(10), "worker")).isEmpty();
        assertThat(repository.tryClaim(activeLock.id(), NOW, NOW.plusSeconds(10), "worker")).isEmpty();
    }

    @Test
    void wrongOwnerFailureDoesNotChangeTheClaimedMessage() {
        OutboxMessage message = pending(UUID.randomUUID().toString(), NOW.minusSeconds(1), NOW.minusSeconds(1), null, null, 0,
                null);
        persistCommitted(message);
        assertThat(repository.tryClaim(message.id(), NOW, NOW.plusSeconds(30), "owner")).isPresent();
        OutboxMessage before = findCommitted(message.id());

        assertThat(repository.rescheduleAfterFailureIfOwned(message.id(), "other-owner", NOW.plusSeconds(5), "failure"))
                .isFalse();

        assertThat(findCommitted(message.id())).isEqualTo(before);
    }

    private OutboxMessage pending(String id, Instant created, Instant availableAt, Instant lockedUntil, String lockedBy,
            int attempts, String lastError) {
        return new OutboxMessage(UUID.fromString(id), "bc3.SubscriptionCreated", "{\"subscriptionId\":\"42\"}", created,
                false, null, attempts, lastError, availableAt, lockedUntil, lockedBy, "correlation-1",
                "0123456789abcdef0123456789abcdef", "0123456789abcdef");
    }

    private static void assertValidation(org.assertj.core.api.ThrowableAssert.ThrowingCallable invocation,
            Class<? extends Throwable> expectedType) {
        assertValidation(invocation, expectedType, null);
    }

    private static void assertValidation(org.assertj.core.api.ThrowableAssert.ThrowingCallable invocation,
            Class<? extends Throwable> expectedType, String expectedMessage) {
        assertThatThrownBy(invocation).satisfies(thrown -> {
            Throwable root = thrown;
            while (root.getCause() != null) {
                root = root.getCause();
            }
            assertThat(root).isInstanceOf(expectedType);
            if (expectedMessage != null) {
                assertThat(root).hasMessage(expectedMessage);
            }
        });
    }

    private void persistCommitted(OutboxMessage message) {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transaction.executeWithoutResult(status -> repository.save(message));
    }

    private OutboxMessage findCommitted(UUID id) {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return transaction.execute(status -> repository.findById(id).orElseThrow());
    }
}
