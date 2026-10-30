package com.mcalvaro.mscatering.infrastructure.outbox;

import com.mcalvaro.mscatering.application.integration.IntegrationEventPublisher;
import com.mcalvaro.mscatering.application.outbox.OutboxMessageRepository;
import com.mcalvaro.mscatering.infrastructure.persistence.outbox.codec.JacksonDomainEventCodec;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class OutboxRelayConfigurationTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(OutboxRelayPropertiesConfiguration.class, OutboxRelayConfiguration.class,
                    RelayDependencies.class);

    @Test
    void disabledByDefaultCreatesNoProcessorRelayOrSchedulingAndBindsDefaults() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(OutboxProcessor.class);
            assertThat(context).doesNotHaveBean(OutboxRelay.class);
            assertThat(context).doesNotHaveBean(ScheduledAnnotationBeanPostProcessor.class);
            assertThat(context.getBean(OutboxRelayProperties.class))
                    .isEqualTo(new OutboxRelayProperties("none", false, Duration.ofSeconds(5), 20,
                            Duration.ofMinutes(1), Duration.ofSeconds(5), Duration.ofMinutes(5)));
        });
    }

    @Test
    void bindsCustomRelayPropertiesAndRejectsInvalidValues() {
        contextRunner
                .withPropertyValues("outbox.provider=test", "outbox.relay-enabled=false", "outbox.poll-interval=9s",
                        "outbox.batch-size=3", "outbox.lease-duration=17s", "outbox.initial-backoff=11s",
                        "outbox.max-backoff=21s")
                .run(context -> assertThat(context.getBean(OutboxRelayProperties.class))
                        .isEqualTo(new OutboxRelayProperties("test", false, Duration.ofSeconds(9), 3,
                                Duration.ofSeconds(17), Duration.ofSeconds(11), Duration.ofSeconds(21))));
        assertBindingFails("outbox.batch-size=0");
        assertBindingFails("outbox.lease-duration=0s");
        assertBindingFails("outbox.initial-backoff=0s");
        assertBindingFails("outbox.initial-backoff=6m", "outbox.max-backoff=5m");
    }

    @Test
    void rejectsBlankProviderAndNonPositiveRelayIntervalsDuringContextBinding() {
        assertBindingFailsWithMessage("Outbox provider must not be blank", "outbox.provider= ");
        assertBindingFailsWithMessage("Poll interval must be positive", "outbox.poll-interval=0s");
        assertBindingFailsWithMessage("Poll interval must be positive", "outbox.poll-interval=-1s");
        assertBindingFailsWithMessage("Maximum backoff must be positive", "outbox.max-backoff=0s");
        assertBindingFailsWithMessage("Maximum backoff must be positive", "outbox.max-backoff=-1s");
    }

    @Test
    void enabledRelayRejectsNoneMissingOrMultiplePublishers() {
        contextRunner.withUserConfiguration(NoPublisher.class)
                .withPropertyValues("outbox.relay-enabled=true", "outbox.provider=none")
                .run(context -> assertThat(context.getStartupFailure()).hasRootCauseMessage(
                        "Outbox relay cannot be enabled when provider is none"));
        contextRunner.withUserConfiguration(NoPublisher.class)
                .withPropertyValues("outbox.relay-enabled=true", "outbox.provider=test")
                .run(context -> assertThat(context.getStartupFailure()).hasRootCauseMessage(
                        "Outbox relay requires exactly one IntegrationEventPublisher"));
        contextRunner.withUserConfiguration(TwoPublishers.class)
                .withPropertyValues("outbox.relay-enabled=true", "outbox.provider=test")
                .run(context -> assertThat(context.getStartupFailure()).hasRootCauseMessage(
                        "Outbox relay requires exactly one IntegrationEventPublisher"));
    }

    @Test
    void enabledRelayStartsWithExactlyOneTestPublisher() {
        contextRunner.withUserConfiguration(OnePublisher.class)
                .withPropertyValues("outbox.relay-enabled=true", "outbox.provider=test")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(OutboxProcessor.class);
                    assertThat(context).hasSingleBean(OutboxRelay.class);
                    assertThat(context).hasSingleBean(RecordingIntegrationEventPublisher.class);
                    assertThat(context).hasSingleBean(ScheduledAnnotationBeanPostProcessor.class);
                });
    }

    private void assertBindingFails(String... values) {
        contextRunner.withPropertyValues(values).run(context -> assertThat(context.getStartupFailure()).isNotNull());
    }

    private void assertBindingFailsWithMessage(String message, String... values) {
        contextRunner.withPropertyValues(values).run(context -> assertThat(context.getStartupFailure())
                .hasRootCauseMessage(message));
    }

    @Configuration(proxyBeanMethods = false)
    static class RelayDependencies {
        @Bean
        OutboxMessageRepository outboxMessageRepository() {
            return mock(OutboxMessageRepository.class);
        }

        @Bean
        JacksonDomainEventCodec jacksonDomainEventCodec() {
            return new JacksonDomainEventCodec();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class NoPublisher {
    }

    @Configuration(proxyBeanMethods = false)
    static class OnePublisher {
        @Bean
        RecordingIntegrationEventPublisher fakeIntegrationEventPublisher() {
            return new RecordingIntegrationEventPublisher();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class TwoPublishers {
        @Bean
        IntegrationEventPublisher firstFakeIntegrationEventPublisher() {
            return new RecordingIntegrationEventPublisher();
        }

        @Bean
        IntegrationEventPublisher secondFakeIntegrationEventPublisher() {
            return new RecordingIntegrationEventPublisher();
        }
    }
}
