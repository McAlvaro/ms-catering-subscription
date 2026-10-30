package com.mcalvaro.mscatering.infrastructure.integration.inbound.kafka;

import com.mcalvaro.mscatering.infrastructure.integration.inbound.bc1.address.PatientAddressRegistrationInboundAdapter;
import com.mcalvaro.mscatering.infrastructure.integration.inbound.bc1.patient.PatientRegistrationInboundAdapter;
import com.mcalvaro.mscatering.infrastructure.integration.inbound.bc2.dietplan.DietPlanInboundAdapter;
import com.mcalvaro.mscatering.infrastructure.integration.inbound.bc5.delivery.DeliveryInboundAdapter;
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
            assertThat(context).doesNotHaveBean(PatientAddressRegistrationKafkaListener.class);
            assertThat(context).doesNotHaveBean(DeliveryKafkaListener.class);
        });
    }

    @Test
    void defaultKafkaDisabledDoesNotInstantiateListener() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(PatientRegistrationKafkaListener.class);
            assertThat(context).doesNotHaveBean(DietPlanKafkaListener.class);
            assertThat(context).doesNotHaveBean(PatientAddressRegistrationKafkaListener.class);
            assertThat(context).doesNotHaveBean(DeliveryKafkaListener.class);
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

    @Test
    void enabledKafkaRegistersPatientAddressRegistrationKafkaListener() {
        contextRunner.withPropertyValues("app.kafka.enabled=true").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(PatientAddressRegistrationKafkaListener.class);
        });
    }

    @Test
    void enabledKafkaRegistersDeliveryKafkaListener() {
        contextRunner.withPropertyValues("app.kafka.enabled=true").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(DeliveryKafkaListener.class);
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

        @Bean
        PatientAddressRegistrationInboundAdapter patientAddressRegistrationInboundAdapter() {
            return mock(PatientAddressRegistrationInboundAdapter.class);
        }

        @Bean
        DeliveryInboundAdapter deliveryInboundAdapter() {
            return mock(DeliveryInboundAdapter.class);
        }
    }
}
