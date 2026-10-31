package com.mcalvaro.mscatering.infrastructure.events;

import com.mcalvaro.mscatering.application.abstractions.DomainEventNotification;
import com.mcalvaro.mscatering.application.outbox.OutboxMessage;
import com.mcalvaro.mscatering.application.outbox.OutboxMessageRepository;
import com.mcalvaro.mscatering.domain.core.DomainEvent;
import com.mcalvaro.mscatering.domain.core.id.DeliveryDayId;
import com.mcalvaro.mscatering.domain.core.id.DietPlanId;
import com.mcalvaro.mscatering.domain.core.id.PatientId;
import com.mcalvaro.mscatering.domain.core.id.SubscriptionId;
import com.mcalvaro.mscatering.domain.dietplan.PlanType;
import com.mcalvaro.mscatering.domain.subscription.event.DeliveryConfirmed;
import com.mcalvaro.mscatering.domain.subscription.event.SubscriptionCreated;
import com.mcalvaro.mscatering.domain.subscription.vo.ContractCode;
import com.mcalvaro.mscatering.infrastructure.persistence.outbox.codec.EncodedDomainEvent;
import com.mcalvaro.mscatering.infrastructure.persistence.outbox.codec.JacksonDomainEventCodec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SaveDomainEventToOutboxHandlerTest {

    @Mock
    private JacksonDomainEventCodec codec;

    @Mock
    private OutboxMessageRepository repository;

    @InjectMocks
    private SaveDomainEventToOutboxHandler handler;

    @Test
    void persistsSupportedEventWithExactPendingMetadata() {
        SubscriptionCreated event = subscriptionCreated();
        EncodedDomainEvent encoded = new EncodedDomainEvent("subscription.created", "{\"event\":\"created\"}");
        when(codec.supports(event)).thenReturn(true);
        when(codec.encode(event)).thenReturn(encoded);

        Instant beforeHandling = Instant.now();
        handler.handle(new DomainEventNotification(event));
        Instant afterHandling = Instant.now();

        ArgumentCaptor<OutboxMessage> messageCaptor = ArgumentCaptor.forClass(OutboxMessage.class);
        verify(repository).save(messageCaptor.capture());
        OutboxMessage message = messageCaptor.getValue();
        assertThat(message.id()).isEqualTo(event.eventId());
        assertThat(message.type()).isEqualTo(encoded.type());
        assertThat(message.content()).isEqualTo(encoded.content());
        assertThat(message.created()).isEqualTo(message.availableAt());
        assertThat(message.created()).isBetween(beforeHandling, afterHandling);
        assertThat(message.processed()).isFalse();
        assertThat(message.processedOn()).isNull();
        assertThat(message.attemptCount()).isZero();
        assertThat(message.lastError()).isNull();
        assertThat(message.lockedUntil()).isNull();
        assertThat(message.lockedBy()).isNull();
        assertThat(message.correlationId()).isEqualTo(event.correlationId());
        assertThat(message.traceId()).isNull();
        assertThat(message.spanId()).isNull();
        verify(codec).encode(event);
    }

    @Test
    void ignoresUnsupportedInternalEventsWithoutEncodingOrPersisting() {
        DeliveryConfirmed event = new DeliveryConfirmed(SubscriptionId.of("SUS-test-1"), DeliveryDayId.of("CD-test-1"));
        when(codec.supports(event)).thenReturn(false);

        handler.handle(new DomainEventNotification(event));

        verify(codec).supports(event);
        verify(codec, never()).encode(any());
        verify(repository, never()).save(any());
    }

    @Test
    void propagatesRepositoryRuntimeExceptionUnchanged() {
        SubscriptionCreated event = subscriptionCreated();
        RuntimeException failure = new RuntimeException("duplicate outbox id");
        when(codec.supports(event)).thenReturn(true);
        when(codec.encode(event)).thenReturn(new EncodedDomainEvent("subscription.created", "{\"event\":\"created\"}"));
        doThrow(failure).when(repository).save(any());

        assertThatThrownBy(() -> handler.handle(new DomainEventNotification(event))).isSameAs(failure);
    }

    @Test
    void rejectsARegistryInconsistencyBeforeEncodingOrPersisting() {
        DomainEvent uncorrelatedEvent = new UncorrelatedEvent(UUID.randomUUID(), Instant.parse("2026-10-29T10:15:30Z"));
        when(codec.supports(uncorrelatedEvent)).thenReturn(true);

        assertThatIllegalStateException()
                .isThrownBy(() -> handler.handle(new DomainEventNotification(uncorrelatedEvent)));

        verify(codec, never()).encode(any());
        verify(repository, never()).save(any());
    }

    private static SubscriptionCreated subscriptionCreated() {
        return new SubscriptionCreated(UUID.fromString("00000000-0000-0000-0000-000000000001"),
                Instant.parse("2026-10-29T10:15:30.123456Z"),
                SubscriptionId.of("SUS-00000000-0000-0000-0000-000000000002"),
                PatientId.of("PAC-00000000-0000-0000-0000-000000000003"), new ContractCode("NTC-2026-0001"),
                DietPlanId.of("PLAN-00000000-0000-0000-0000-000000000004"), PlanType.BASE,
                LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 24), 15, List.of(), new BigDecimal("249.90"),
                "correlation-created");
    }

    private record UncorrelatedEvent(UUID eventId, Instant occurredAt) implements DomainEvent {
    }
}
