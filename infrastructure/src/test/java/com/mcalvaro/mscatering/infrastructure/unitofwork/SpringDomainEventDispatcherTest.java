package com.mcalvaro.mscatering.infrastructure.unitofwork;

import an.awesome.pipelinr.Pipeline;
import com.mcalvaro.mscatering.application.abstractions.DomainEventNotification;
import com.mcalvaro.mscatering.domain.core.AggregateRoot;
import com.mcalvaro.mscatering.domain.core.DomainEvent;
import com.mcalvaro.mscatering.domain.core.id.SubscriptionId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SpringDomainEventDispatcherTest {

    @Mock
    private Pipeline pipeline;

    @Test
    void clearsPendingEventsAfterSuccessfulPipelineSend() {
        TestAggregate aggregate = new TestAggregate();
        DomainEvent event = new TestEvent(UUID.randomUUID(), Instant.parse("2026-10-29T10:15:30Z"));
        aggregate.record(event);
        SpringDomainEventDispatcher dispatcher = new SpringDomainEventDispatcher(pipeline);
        dispatcher.register(aggregate);

        dispatcher.dispatch();

        ArgumentCaptor<DomainEventNotification> notificationCaptor = ArgumentCaptor.forClass(DomainEventNotification.class);
        verify(pipeline).send(notificationCaptor.capture());
        assertThat(notificationCaptor.getValue().domainEvent()).isSameAs(event);
        assertThat(aggregate.getDomainEvents()).isEmpty();
    }

    @Test
    void propagatesPipelineFailureAndRetainsPendingEvents() {
        TestAggregate aggregate = new TestAggregate();
        DomainEvent event = new TestEvent(UUID.randomUUID(), Instant.parse("2026-10-29T10:15:30Z"));
        aggregate.record(event);
        SpringDomainEventDispatcher dispatcher = new SpringDomainEventDispatcher(pipeline);
        dispatcher.register(aggregate);
        RuntimeException failure = new RuntimeException("outbox insertion failed");
        doThrow(failure).when(pipeline).send(any(DomainEventNotification.class));

        assertThatThrownBy(dispatcher::dispatch).isSameAs(failure);

        assertThat(aggregate.getDomainEvents()).containsExactly(event);
    }

    private static final class TestAggregate extends AggregateRoot<SubscriptionId> {
        private TestAggregate() {
            super(SubscriptionId.of("SUS-test-aggregate"));
        }

        private void record(DomainEvent event) {
            addDomainEvent(event);
        }
    }

    private record TestEvent(UUID eventId, Instant occurredAt) implements DomainEvent {
    }
}
