package com.mcalvaro.mscatering.infrastructure.outbox;

import com.mcalvaro.mscatering.application.integration.IntegrationEventPublisher;
import com.mcalvaro.mscatering.application.outbox.OutboxMessageRepository;
import com.mcalvaro.mscatering.application.outbox.translation.ConsolidatedCalendarClosedOutboxEventTranslator;
import com.mcalvaro.mscatering.application.outbox.translation.OutboxEventTranslatorRegistry;
import com.mcalvaro.mscatering.application.outbox.translation.SubscriptionCancelledOutboxEventTranslator;
import com.mcalvaro.mscatering.application.outbox.translation.SubscriptionCompletedOutboxEventTranslator;
import com.mcalvaro.mscatering.application.outbox.translation.SubscriptionCreatedOutboxEventTranslator;
import com.mcalvaro.mscatering.application.outbox.translation.SubscriptionPausedOutboxEventTranslator;
import com.mcalvaro.mscatering.application.outbox.translation.SubscriptionReactivatedOutboxEventTranslator;
import com.mcalvaro.mscatering.infrastructure.persistence.outbox.codec.JacksonDomainEventCodec;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;
import java.util.List;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnProperty(prefix = "outbox", name = "relay-enabled", havingValue = "true")
public class OutboxRelayConfiguration {

    @Bean
    Clock outboxClock() {
        return Clock.systemUTC();
    }

    @Bean
    OutboxEventTranslatorRegistry outboxEventTranslatorRegistry() {
        return new OutboxEventTranslatorRegistry(List.of(
                new SubscriptionCreatedOutboxEventTranslator(), new SubscriptionPausedOutboxEventTranslator(),
                new SubscriptionReactivatedOutboxEventTranslator(), new SubscriptionCancelledOutboxEventTranslator(),
                new SubscriptionCompletedOutboxEventTranslator(),
                new ConsolidatedCalendarClosedOutboxEventTranslator()));
    }

    @Bean
    OutboxProcessor outboxProcessor(OutboxMessageRepository repository, JacksonDomainEventCodec codec,
            OutboxEventTranslatorRegistry translators, List<IntegrationEventPublisher> publishers, Clock outboxClock,
            OutboxRelayProperties properties) {
        if ("none".equalsIgnoreCase(properties.provider())) {
            throw new IllegalStateException("Outbox relay cannot be enabled when provider is none");
        }
        if (publishers.size() != 1) {
            throw new IllegalStateException("Outbox relay requires exactly one IntegrationEventPublisher");
        }
        return new OutboxProcessor(repository, codec, translators, publishers.get(0), outboxClock,
                properties.initialBackoff(), properties.maxBackoff());
    }

    @Bean
    OutboxRelay outboxRelay(OutboxProcessor processor, OutboxRelayProperties properties) {
        return new OutboxRelay(processor, properties);
    }
}
