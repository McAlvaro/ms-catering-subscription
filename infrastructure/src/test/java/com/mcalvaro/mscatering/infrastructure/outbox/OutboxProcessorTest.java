package com.mcalvaro.mscatering.infrastructure.outbox;

import com.mcalvaro.mscatering.application.integration.IntegrationEventPublisher;
import com.mcalvaro.mscatering.application.outbox.OutboxMessage;
import com.mcalvaro.mscatering.application.outbox.OutboxMessageRepository;
import com.mcalvaro.mscatering.application.outbox.translation.OutboxEventTranslatorRegistry;
import com.mcalvaro.mscatering.domain.core.DomainEvent;
import com.mcalvaro.mscatering.domain.core.id.PatientId;
import com.mcalvaro.mscatering.domain.core.id.SubscriptionId;
import com.mcalvaro.mscatering.domain.subscription.event.SubscriptionCreated;
import com.mcalvaro.mscatering.domain.subscription.vo.ContractCode;
import com.mcalvaro.mscatering.infrastructure.persistence.outbox.codec.JacksonDomainEventCodec;
import com.mcalvaro.mscatering.integration.contracts.IntegrationEvent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class OutboxProcessorTest {
    private static final Instant NOW = Instant.parse("2026-10-29T12:00:00Z");
    private static final Duration LEASE = Duration.ofMinutes(1);

    @Test
    void publishesDecodedAndTranslatedMessageThenAcknowledgesTheExactLeaseInOrder() {
        OutboxMessageRepository repository = mock(OutboxMessageRepository.class);
        JacksonDomainEventCodec codec = mock(JacksonDomainEventCodec.class);
        OutboxEventTranslatorRegistry translators = mock(OutboxEventTranslatorRegistry.class);
        IntegrationEventPublisher publisher = mock(IntegrationEventPublisher.class);
        OutboxMessage message = message(0);
        DomainEvent decoded = decodedEvent();
        IntegrationEvent translated = translatedEvent();
        when(repository.findPendingAvailable(NOW, 20)).thenReturn(List.of(message));
        when(repository.tryClaim(eq(message.id()), eq(NOW), eq(NOW.plus(LEASE)), anyString()))
                .thenReturn(Optional.of(message));
        when(codec.decode(message.type(), message.content())).thenReturn(decoded);
        when(translators.translate(decoded)).thenReturn(translated);

        new OutboxProcessor(repository, codec, translators, publisher, Clock.fixed(NOW, ZoneOffset.UTC))
                .processAvailable(20, LEASE);

        ArgumentCaptor<String> token = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Instant> processedOn = ArgumentCaptor.forClass(Instant.class);
        verify(repository).tryClaim(eq(message.id()), eq(NOW), eq(NOW.plus(LEASE)), token.capture());
        assertThat(UUID.fromString(token.getValue())).isNotNull();
        verify(repository).markProcessedIfOwned(eq(message.id()), eq(token.getValue()), processedOn.capture());
        assertThat(processedOn.getValue()).isEqualTo(NOW);
        verify(codec).decode(message.type(), message.content());
        verify(translators).translate(decoded);
        verify(publisher).publish(translated);
        InOrder order = inOrder(repository, codec, translators, publisher);
        order.verify(repository).findPendingAvailable(NOW, 20);
        order.verify(repository).tryClaim(eq(message.id()), eq(NOW), eq(NOW.plus(LEASE)), eq(token.getValue()));
        order.verify(codec).decode(message.type(), message.content());
        order.verify(translators).translate(decoded);
        order.verify(publisher).publish(translated);
        order.verify(repository).markProcessedIfOwned(message.id(), token.getValue(), NOW);
        verifyNoMoreInteractions(repository, codec, translators, publisher);
    }

    @Test
    void fetchesFreshTimeImmediatelyBeforeEveryClaimAndDerivesAnUnexpiredLeaseFromIt() {
        OutboxMessageRepository repository = mock(OutboxMessageRepository.class);
        JacksonDomainEventCodec codec = mock(JacksonDomainEventCodec.class);
        OutboxEventTranslatorRegistry translators = mock(OutboxEventTranslatorRegistry.class);
        IntegrationEventPublisher publisher = mock(IntegrationEventPublisher.class);
        OutboxMessage first = message(0);
        OutboxMessage second = message(0);
        DomainEvent decoded = decodedEvent();
        IntegrationEvent translated = translatedEvent();
        Instant secondClaimTime = NOW.plus(Duration.ofMinutes(2));
        when(repository.findPendingAvailable(NOW, 2)).thenReturn(List.of(first, second));
        when(repository.tryClaim(eq(first.id()), any(), any(), anyString())).thenReturn(Optional.of(first));
        when(repository.tryClaim(eq(second.id()), any(), any(), anyString())).thenReturn(Optional.of(second));
        when(codec.decode(anyString(), anyString())).thenReturn(decoded);
        when(translators.translate(decoded)).thenReturn(translated);

        new OutboxProcessor(repository, codec, translators, publisher,
                new SequenceClock(NOW, NOW, NOW, secondClaimTime, secondClaimTime))
                .processAvailable(2, LEASE);

        ArgumentCaptor<Instant> claims = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> leases = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<String> tokens = ArgumentCaptor.forClass(String.class);
        verify(repository, org.mockito.Mockito.times(2)).tryClaim(any(), claims.capture(), leases.capture(),
                tokens.capture());
        assertThat(claims.getAllValues()).containsExactly(NOW, secondClaimTime);
        assertThat(leases.getAllValues()).containsExactly(NOW.plus(LEASE), secondClaimTime.plus(LEASE));
        assertThat(leases.getAllValues().get(1)).isAfter(secondClaimTime);
        assertThat(tokens.getAllValues()).allSatisfy(value -> UUID.fromString(value));
        assertThat(tokens.getAllValues()).doesNotHaveDuplicates();
    }

    @Test
    void failedClaimSkipsDecodeTranslationPublicationAndFinalization() {
        OutboxMessageRepository repository = mock(OutboxMessageRepository.class);
        JacksonDomainEventCodec codec = mock(JacksonDomainEventCodec.class);
        OutboxEventTranslatorRegistry translators = mock(OutboxEventTranslatorRegistry.class);
        IntegrationEventPublisher publisher = mock(IntegrationEventPublisher.class);
        OutboxMessage message = message(0);
        when(repository.findPendingAvailable(NOW, 1)).thenReturn(List.of(message));
        when(repository.tryClaim(any(), any(), any(), anyString())).thenReturn(Optional.empty());

        new OutboxProcessor(repository, codec, translators, publisher, Clock.fixed(NOW, ZoneOffset.UTC))
                .processAvailable(1, LEASE);

        verifyNoInteractions(codec, translators, publisher);
        verify(repository, never()).markProcessedIfOwned(any(), anyString(), any());
        verify(repository, never()).rescheduleAfterFailureIfOwned(any(), anyString(), any(), anyString());
    }

    @Test
    void translatorFailureReschedulesTheOwnedMessageWithoutPublishing() {
        assertFailureReschedules(new IllegalStateException("translation failed"), FailurePoint.TRANSLATOR,
                "java.lang.IllegalStateException: translation failed", Duration.ofSeconds(5));
    }

    @Test
    void codecAndPublisherFailuresRescheduleWithConfiguredBackoffAndCappedDelay() {
        assertFailureReschedules(new IllegalArgumentException("bad payload"), FailurePoint.CODEC,
                "java.lang.IllegalArgumentException: bad payload", Duration.ofSeconds(7));
        assertFailureReschedules(new IllegalStateException("publisher down"), FailurePoint.PUBLISHER,
                "java.lang.IllegalStateException: publisher down", Duration.ofSeconds(7));
        assertFailureReschedules(new IllegalStateException("publisher down"), FailurePoint.PUBLISHER,
                "java.lang.IllegalStateException: publisher down", Duration.ofSeconds(30), 10);
    }

    @Test
    void nullAndBlankFailureMessagesUseOnlyTheFullyQualifiedExceptionClass() {
        assertFailureReschedules(new IllegalStateException(), FailurePoint.PUBLISHER,
                "java.lang.IllegalStateException", Duration.ofSeconds(5));
        assertFailureReschedules(new IllegalStateException(" \t"), FailurePoint.PUBLISHER,
                "java.lang.IllegalStateException", Duration.ofSeconds(5));
    }

    @Test
    void failedOrExceptionalPostPublishAcknowledgementIsNotRequeued() {
        for (boolean exceptionalAck : List.of(false, true)) {
            OutboxMessageRepository repository = mock(OutboxMessageRepository.class);
            JacksonDomainEventCodec codec = mock(JacksonDomainEventCodec.class);
            OutboxEventTranslatorRegistry translators = mock(OutboxEventTranslatorRegistry.class);
            IntegrationEventPublisher publisher = mock(IntegrationEventPublisher.class);
            OutboxMessage message = message(0);
            DomainEvent decoded = decodedEvent();
            IntegrationEvent translated = translatedEvent();
            when(repository.findPendingAvailable(NOW, 1)).thenReturn(List.of(message));
            when(repository.tryClaim(any(), any(), any(), anyString())).thenReturn(Optional.of(message));
            when(codec.decode(message.type(), message.content())).thenReturn(decoded);
            when(translators.translate(decoded)).thenReturn(translated);
            if (exceptionalAck) {
                doThrow(new IllegalStateException("database unavailable"))
                        .when(repository).markProcessedIfOwned(any(), anyString(), any());
            } else {
                when(repository.markProcessedIfOwned(any(), anyString(), any())).thenReturn(false);
            }

            new OutboxProcessor(repository, codec, translators, publisher, Clock.fixed(NOW, ZoneOffset.UTC))
                    .processAvailable(1, LEASE);

            verify(publisher).publish(translated);
            verify(repository, never()).rescheduleAfterFailureIfOwned(any(), anyString(), any(), anyString());
        }
    }

    @Test
    void wrongOwnerFailureRescheduleIsAttemptedOnceAndLeavesRecoveryToLeaseExpiry() {
        OutboxMessageRepository repository = mock(OutboxMessageRepository.class);
        JacksonDomainEventCodec codec = mock(JacksonDomainEventCodec.class);
        OutboxEventTranslatorRegistry translators = mock(OutboxEventTranslatorRegistry.class);
        IntegrationEventPublisher publisher = mock(IntegrationEventPublisher.class);
        OutboxMessage message = message(0);
        when(repository.findPendingAvailable(NOW, 1)).thenReturn(List.of(message));
        when(repository.tryClaim(any(), any(), any(), anyString())).thenReturn(Optional.of(message));
        when(codec.decode(anyString(), anyString())).thenThrow(new IllegalStateException("decode failed"));
        when(repository.rescheduleAfterFailureIfOwned(any(), anyString(), any(), anyString())).thenReturn(false);

        new OutboxProcessor(repository, codec, translators, publisher, Clock.fixed(NOW, ZoneOffset.UTC))
                .processAvailable(1, LEASE);

        verify(repository).rescheduleAfterFailureIfOwned(eq(message.id()), anyString(), eq(NOW.plusSeconds(5)),
                eq("java.lang.IllegalStateException: decode failed"));
        verifyNoInteractions(translators, publisher);
    }

    @Test
    void invokesThePublisherOutsideAnActualSpringTransaction() {
        OutboxMessageRepository repository = mock(OutboxMessageRepository.class);
        JacksonDomainEventCodec codec = mock(JacksonDomainEventCodec.class);
        OutboxEventTranslatorRegistry translators = mock(OutboxEventTranslatorRegistry.class);
        OutboxMessage message = message(0);
        DomainEvent decoded = decodedEvent();
        IntegrationEvent translated = translatedEvent();
        when(repository.findPendingAvailable(NOW, 1)).thenReturn(List.of(message));
        when(repository.tryClaim(any(), any(), any(), anyString())).thenReturn(Optional.of(message));
        when(codec.decode(message.type(), message.content())).thenReturn(decoded);
        when(translators.translate(decoded)).thenReturn(translated);
        IntegrationEventPublisher publisher = event -> assertThat(
                TransactionSynchronizationManager.isActualTransactionActive())
                .isFalse();

        new OutboxProcessor(repository, codec, translators, publisher, Clock.fixed(NOW, ZoneOffset.UTC))
                .processAvailable(1, LEASE);
    }

    @Test
    void backoffStartsAtFiveSecondsAndCapsAtFiveMinutes() {
        assertThat(OutboxProcessor.backoffFor(0)).isEqualTo(Duration.ofSeconds(5));
        assertThat(OutboxProcessor.backoffFor(1)).isEqualTo(Duration.ofSeconds(10));
        assertThat(OutboxProcessor.backoffFor(10)).isEqualTo(Duration.ofMinutes(5));
    }

    private static void assertFailureReschedules(Exception failure, FailurePoint failurePoint, String expectedError,
            Duration expectedDelay) {
        assertFailureReschedules(failure, failurePoint, expectedError, expectedDelay, 0);
    }

    private static void assertFailureReschedules(Exception failure, FailurePoint failurePoint, String expectedError,
            Duration expectedDelay, int attempts) {
        OutboxMessageRepository repository = mock(OutboxMessageRepository.class);
        JacksonDomainEventCodec codec = mock(JacksonDomainEventCodec.class);
        OutboxEventTranslatorRegistry translators = mock(OutboxEventTranslatorRegistry.class);
        IntegrationEventPublisher publisher = mock(IntegrationEventPublisher.class);
        OutboxMessage message = message(attempts);
        DomainEvent decoded = decodedEvent();
        IntegrationEvent translated = translatedEvent();
        when(repository.findPendingAvailable(NOW, 1)).thenReturn(List.of(message));
        when(repository.tryClaim(any(), any(), any(), anyString())).thenReturn(Optional.of(message));
        when(codec.decode(message.type(), message.content())).thenReturn(decoded);
        when(translators.translate(decoded)).thenReturn(translated);
        if (failurePoint == FailurePoint.CODEC) {
            when(codec.decode(message.type(), message.content())).thenThrow(failure);
        } else if (failurePoint == FailurePoint.TRANSLATOR) {
            when(translators.translate(decoded)).thenThrow(failure);
        } else {
            doThrow(failure).when(publisher).publish(translated);
        }
        OutboxProcessor processor = (expectedDelay.equals(Duration.ofSeconds(7))
                || expectedDelay.equals(Duration.ofSeconds(30)))
                        ? new OutboxProcessor(repository, codec, translators, publisher,
                                Clock.fixed(NOW, ZoneOffset.UTC),
                                Duration.ofSeconds(7), Duration.ofSeconds(30))
                        : new OutboxProcessor(repository, codec, translators, publisher,
                                Clock.fixed(NOW, ZoneOffset.UTC));

        processor.processAvailable(1, LEASE);

        verify(repository).rescheduleAfterFailureIfOwned(eq(message.id()), anyString(),
                eq(NOW.plus(expectedDelay)), eq(expectedError));
        verify(repository, never()).markProcessedIfOwned(any(), anyString(), any());
        if (failurePoint != FailurePoint.PUBLISHER) {
            verifyNoInteractions(publisher);
        }
    }

    private static SubscriptionCreated decodedEvent() {
        return new SubscriptionCreated(UUID.fromString("00000000-0000-0000-0000-000000000011"),
                NOW.minusSeconds(1), SubscriptionId.of("SUS-00000000-0000-0000-0000-000000000012"),
                PatientId.of("PAC-00000000-0000-0000-0000-000000000013"), ContractCode.generate(2026, 1), null, null,
                null, null, 0, List.of(), null, "correlation-1");
    }

    private static IntegrationEvent translatedEvent() {
        return new TestIntegrationEvent(UUID.fromString("00000000-0000-0000-0000-000000000021"), NOW,
                "correlation-1");
    }

    private static OutboxMessage message(int attempts) {
        return new OutboxMessage(UUID.randomUUID(), "subscription.created", "{\"event\":true}", NOW.minusSeconds(1),
                false, null, attempts, null, NOW.minusSeconds(1), null, null, null, null, null);
    }

    private enum FailurePoint {
        CODEC, TRANSLATOR, PUBLISHER
    }

    private record TestIntegrationEvent(UUID eventId, Instant occurredOn, String correlationId)
            implements IntegrationEvent {
        @Override
        public String eventName() {
            return "test.event";
        }

        @Override
        public String logicalDestination() {
            return "test";
        }
    }

    private static final class SequenceClock extends Clock {
        private final Iterator<Instant> values;

        private SequenceClock(Instant... values) {
            this.values = List.of(values).iterator();
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return values.next();
        }
    }
}
