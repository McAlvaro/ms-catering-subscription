package com.mcalvaro.mscatering.infrastructure.integration.outbound.kafka;

import com.mcalvaro.mscatering.application.integration.IntegrationEventPublisher;
import com.mcalvaro.mscatering.infrastructure.outbox.OutboxRelayProperties;
import com.mcalvaro.mscatering.infrastructure.outbox.OutboxRelayPropertiesConfiguration;
import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class KafkaIntegrationConfigurationTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(KafkaIntegrationConfiguration.class, KafkaDependencies.class,
                    OutboxRelayPropertiesConfiguration.class);

    @Test
    void disabledKafkaCreatesNoPublisherOrTopicAndPreservesRelayDefaults() {
        contextRunner.withPropertyValues("app.kafka.enabled=false").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(IntegrationEventPublisher.class);
            assertThat(context).doesNotHaveBean(NewTopic.class);
            assertThat(context.getBean(OutboxRelayProperties.class))
                    .isEqualTo(new OutboxRelayProperties("none", false, Duration.ofSeconds(5), 20,
                            Duration.ofMinutes(1), Duration.ofSeconds(5), Duration.ofMinutes(5)));
        });
    }

    @Test
    void enabledKafkaCreatesExactlyOnePublisherAndTheThreePartitionTopic() {
        contextRunner.withPropertyValues("app.kafka.enabled=true", "app.kafka.topic=bc3.catering.eventos").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(IntegrationEventPublisher.class);
            assertThat(context).hasSingleBean(NewTopic.class);
            NewTopic topic = context.getBean(NewTopic.class);
            assertThat(topic.name()).isEqualTo("bc3.catering.eventos");
            assertThat(topic.numPartitions()).isEqualTo(3);
            assertThat(topic.replicationFactor()).isEqualTo((short) 1);
        });
    }

    @Configuration(proxyBeanMethods = false)
    static class KafkaDependencies {
        @Bean
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, String> kafkaTemplate() {
            return mock(KafkaTemplate.class);
        }
    }
}
