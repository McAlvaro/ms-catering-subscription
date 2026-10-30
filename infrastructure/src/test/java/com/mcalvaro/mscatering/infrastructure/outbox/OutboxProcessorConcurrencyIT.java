package com.mcalvaro.mscatering.infrastructure.outbox;

import com.mcalvaro.mscatering.application.outbox.OutboxMessage;
import com.mcalvaro.mscatering.application.outbox.OutboxMessageRepository;
import com.mcalvaro.mscatering.application.outbox.translation.ConsolidatedCalendarClosedOutboxEventTranslator;
import com.mcalvaro.mscatering.application.outbox.translation.OutboxEventTranslatorRegistry;
import com.mcalvaro.mscatering.application.outbox.translation.SubscriptionCancelledOutboxEventTranslator;
import com.mcalvaro.mscatering.application.outbox.translation.SubscriptionCompletedOutboxEventTranslator;
import com.mcalvaro.mscatering.application.outbox.translation.SubscriptionCreatedOutboxEventTranslator;
import com.mcalvaro.mscatering.application.outbox.translation.SubscriptionPausedOutboxEventTranslator;
import com.mcalvaro.mscatering.application.outbox.translation.SubscriptionReactivatedOutboxEventTranslator;
import com.mcalvaro.mscatering.domain.dietplan.PlanType;
import com.mcalvaro.mscatering.domain.subscription.event.SubscriptionCreated;
import com.mcalvaro.mscatering.domain.subscription.vo.ContractCode;
import com.mcalvaro.mscatering.infrastructure.BaseIntegrationTest;
import com.mcalvaro.mscatering.infrastructure.MsCateringApplication;
import com.mcalvaro.mscatering.infrastructure.persistence.outbox.codec.JacksonDomainEventCodec;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = MsCateringApplication.class, properties = "spring.datasource.url=jdbc:h2:mem:outbox_processor_concurrency;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE")
class OutboxProcessorConcurrencyIT extends BaseIntegrationTest {
    @Autowired
    private OutboxMessageRepository repository;
    @Autowired
    private JacksonDomainEventCodec codec;
    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void concurrentProcessorsPublishOnlyOnceWhileTheWinningLeaseIsValid() throws Exception {
        SubscriptionCreated source = sourceEvent();
        var encoded = codec.encode(source);
        OutboxMessage message = OutboxMessage.pending(UUID.randomUUID(), encoded.type(), encoded.content(),
                Instant.now().minusSeconds(1));
        persistCommitted(message);
        RecordingIntegrationEventPublisher publisher = new RecordingIntegrationEventPublisher();
        CyclicBarrier bothCandidatesRead = new CyclicBarrier(2);
        OutboxEventTranslatorRegistry translators = translators();
        OutboxProcessor first = new OutboxProcessor(new BarrierRepository(repository, bothCandidatesRead), codec,
                translators,
                publisher, Clock.systemUTC());
        OutboxProcessor second = new OutboxProcessor(new BarrierRepository(repository, bothCandidatesRead), codec,
                translators,
                publisher, Clock.systemUTC());

        var executor = Executors.newFixedThreadPool(2);
        try {
            var results = executor.invokeAll(List.of(
                    () -> {
                        first.processAvailable(1, java.time.Duration.ofMinutes(1));
                        return null;
                    },
                    () -> {
                        second.processAvailable(1, java.time.Duration.ofMinutes(1));
                        return null;
                    }));
            for (var result : results) {
                result.get();
            }
        } finally {
            executor.shutdownNow();
        }

        assertThat(publisher.published()).hasSize(1);
        assertThat(publisher.published().get(0).eventId()).isEqualTo(source.eventId());
        assertThat(findCommitted(message.id()).processed()).isTrue();
    }

    private OutboxEventTranslatorRegistry translators() {
        return new OutboxEventTranslatorRegistry(List.of(new SubscriptionCreatedOutboxEventTranslator(),
                new SubscriptionPausedOutboxEventTranslator(), new SubscriptionReactivatedOutboxEventTranslator(),
                new SubscriptionCancelledOutboxEventTranslator(), new SubscriptionCompletedOutboxEventTranslator(),
                new ConsolidatedCalendarClosedOutboxEventTranslator()));
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

    private static SubscriptionCreated sourceEvent() {
        return new SubscriptionCreated(UUID.randomUUID(), Instant.now().minusSeconds(2), UUID.randomUUID(),
                UUID.randomUUID(),
                ContractCode.generate(2026, 1), UUID.randomUUID(), PlanType.BASE, LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 15), 15, List.of(), BigDecimal.TEN, "relay-concurrency");
    }

    private static final class BarrierRepository implements OutboxMessageRepository {
        private final OutboxMessageRepository delegate;
        private final CyclicBarrier barrier;

        private BarrierRepository(OutboxMessageRepository delegate, CyclicBarrier barrier) {
            this.delegate = delegate;
            this.barrier = barrier;
        }

        @Override
        public void save(OutboxMessage message) {
            delegate.save(message);
        }

        @Override
        public Optional<OutboxMessage> findById(UUID id) {
            return delegate.findById(id);
        }

        @Override
        public List<OutboxMessage> findPendingAvailable(Instant now, int limit) {
            List<OutboxMessage> candidates = delegate.findPendingAvailable(now, limit);
            try {
                barrier.await();
            } catch (Exception exception) {
                throw new IllegalStateException("Could not synchronize concurrent processors", exception);
            }
            return candidates;
        }

        @Override
        public Optional<OutboxMessage> tryClaim(UUID id, Instant now, Instant leaseUntil, String lockToken) {
            return delegate.tryClaim(id, now, leaseUntil, lockToken);
        }

        @Override
        public boolean markProcessedIfOwned(UUID id, String lockToken, Instant processedOn) {
            return delegate.markProcessedIfOwned(id, lockToken, processedOn);
        }

        @Override
        public boolean rescheduleAfterFailureIfOwned(UUID id, String lockToken, Instant availableAt, String lastError) {
            return delegate.rescheduleAfterFailureIfOwned(id, lockToken, availableAt, lastError);
        }
    }
}
