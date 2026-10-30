package com.mcalvaro.mscatering.infrastructure.integration.inbound.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcalvaro.mscatering.infrastructure.integration.inbound.bc1.patient.PatientRegistrationInboundAdapter;
import com.mcalvaro.mscatering.infrastructure.integration.inbound.bc2.dietplan.DietPlanInboundAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class KafkaInboundConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(KafkaInboundConfiguration.class, InboundDependencies.class);

    @Test
    void disabledKafkaDoesNotInstantiateListener() {
        contextRunner.withPropertyValues("app.kafka.enabled=false").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(PatientRegistrationKafkaListener.class);
            assertThat(context).doesNotHaveBean(DietPlanKafkaListener.class);
        });
    }

    @Test
    void defaultKafkaDisabledDoesNotInstantiateListener() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(PatientRegistrationKafkaListener.class);
            assertThat(context).doesNotHaveBean(DietPlanKafkaListener.class);
        });
    }

    @Test
    void enabledKafkaRegistersPatientRegistrationKafkaListener() {
        contextRunner.withPropertyValues("app.kafka.enabled=true").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(PatientRegistrationKafkaListener.class);
        });
    }

    @Test
    void enabledKafkaRegistersDietPlanKafkaListener() {
        contextRunner.withPropertyValues("app.kafka.enabled=true").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(DietPlanKafkaListener.class);
        });
    }

    @Configuration(proxyBeanMethods = false)
    static class InboundDependencies {
        @Bean
        PatientRegistrationInboundAdapter patientRegistrationInboundAdapter() {
            return mock(PatientRegistrationInboundAdapter.class);
        }

        @Bean
        DietPlanInboundAdapter dietPlanInboundAdapter() {
            return mock(DietPlanInboundAdapter.class);
        }
    }
}
