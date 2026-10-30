package com.mcalvaro.mscatering.infrastructure.outbox;

import com.mcalvaro.mscatering.application.outbox.OutboxMessage;
import com.mcalvaro.mscatering.application.outbox.OutboxMessageRepository;
import com.mcalvaro.mscatering.domain.dietplan.PlanType;
import com.mcalvaro.mscatering.domain.subscription.event.SubscriptionCreated;
import com.mcalvaro.mscatering.domain.subscription.vo.ContractCode;
import com.mcalvaro.mscatering.infrastructure.MsCateringApplication;
import com.mcalvaro.mscatering.infrastructure.persistence.outbox.codec.EncodedDomainEvent;
import com.mcalvaro.mscatering.infrastructure.persistence.outbox.codec.JacksonDomainEventCodec;
import com.mcalvaro.mscatering.integration.contracts.IntegrationEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = MsCateringApplication.class, properties = {
        "outbox.relay-enabled=true", "outbox.provider=test", "outbox.poll-interval=1h",
        "spring.datasource.url=jdbc:h2:mem:outbox_relay_e2e;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE" })
@ActiveProfiles("test")
@Import(OutboxRelayEndToEndIT.FakePublisherConfiguration.class)
class OutboxRelayEndToEndIT {
    @Autowired
    private OutboxProcessor processor;
    @Autowired
    private OutboxMessageRepository repository;
    @Autowired
    private JacksonDomainEventCodec codec;
    @Autowired
    private RecordingIntegrationEventPublisher publisher;
    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void resetPublisher() {
        publisher.reset();
    }

    @Test
    void processesAPersistedPendingMessageWithTheRealCodecTranslatorAndTestPublisher() {
        SubscriptionCreated source = sourceEvent();
        OutboxMessage message = persistPending(source);

        processor.processAvailable(10, java.time.Duration.ofMinutes(1));

        assertThat(publisher.published()).hasSize(1);
        IntegrationEvent published = publisher.published().get(0);
        assertThat(published.eventId()).isEqualTo(source.eventId());
        assertThat(publisher.transactionActiveAtInvocation()).isFalse();
        OutboxMessage persisted = findCommitted(message.id());
        assertThat(persisted.processed()).isTrue();
        assertThat(persisted.processedOn()).isNotNull();
        assertThat(persisted.lockedBy()).isNull();
        assertThat(persisted.lockedUntil()).isNull();
        assertThat(persisted.lastError()).isNull();
    }

    @Test
    void fakePublisherFailurePersistsAttemptErrorAndBackoff() {
        OutboxMessage message = persistPending(sourceEvent());
        publisher.failWith(new IllegalStateException("test transport failed"));
        Instant before = Instant.now();

        processor.processAvailable(10, java.time.Duration.ofMinutes(1));

        Instant after = Instant.now();
        assertThat(publisher.published()).isEmpty();
        assertThat(findCommitted(message.id()))
                .extracting(OutboxMessage::processed, OutboxMessage::attemptCount, OutboxMessage::lastError,
                        OutboxMessage::lockedBy, OutboxMessage::lockedUntil)
                .containsExactly(false, 1, "java.lang.IllegalStateException: test transport failed", null, null);
        assertThat(findCommitted(message.id()).availableAt()).isBetween(before.plusSeconds(5), after.plusSeconds(5));
    }

    private OutboxMessage persistPending(SubscriptionCreated source) {
        EncodedDomainEvent encoded = codec.encode(source);
        OutboxMessage message = OutboxMessage.pending(UUID.randomUUID(), encoded.type(), encoded.content(),
                Instant.now().minusSeconds(1));
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transaction.executeWithoutResult(status -> repository.save(message));
        return message;
    }

    private OutboxMessage findCommitted(UUID id) {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return transaction.execute(status -> repository.findById(id).orElseThrow());
    }

    private static SubscriptionCreated sourceEvent() {
        Instant occurredAt = Instant.now().minusSeconds(2);
        return new SubscriptionCreated(UUID.randomUUID(), occurredAt, UUID.randomUUID(), UUID.randomUUID(),
                ContractCode.generate(2026, 1), UUID.randomUUID(), PlanType.BASE, LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 15), 15, List.of(), BigDecimal.TEN, "relay-e2e");
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FakePublisherConfiguration {
        @Bean
        RecordingIntegrationEventPublisher fakeIntegrationEventPublisher() {
            return new RecordingIntegrationEventPublisher();
        }
    }
}
