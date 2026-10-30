package com.mcalvaro.mscatering.infrastructure.integration.inbound.kafka;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mcalvaro.mscatering.infrastructure.integration.inbound.bc1.address.PatientAddressRegistrationInboundAdapter;
import com.mcalvaro.mscatering.infrastructure.integration.inbound.bc1.patient.PatientRegistrationInboundAdapter;
import com.mcalvaro.mscatering.infrastructure.integration.inbound.bc2.dietplan.DietPlanInboundAdapter;
import com.mcalvaro.mscatering.infrastructure.integration.inbound.bc5.delivery.DeliveryInboundAdapter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Kafka inbound integration configuration for external BC event consumers.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "app.kafka", name = "enabled", havingValue = "true")
public class KafkaInboundConfiguration {

    @Bean
    PatientRegistrationKafkaListener patientRegistrationKafkaListener(
            PatientRegistrationInboundAdapter patientRegistrationInboundAdapter) {
        return new PatientRegistrationKafkaListener(patientRegistrationInboundAdapter, inboundEventObjectMapper());
    }

    @Bean
    DietPlanKafkaListener dietPlanKafkaListener(
            DietPlanInboundAdapter dietPlanInboundAdapter) {
        return new DietPlanKafkaListener(dietPlanInboundAdapter, inboundEventObjectMapper());
    }

    @Bean
    PatientAddressRegistrationKafkaListener patientAddressRegistrationKafkaListener(
            PatientAddressRegistrationInboundAdapter patientAddressRegistrationInboundAdapter) {
        return new PatientAddressRegistrationKafkaListener(
                patientAddressRegistrationInboundAdapter, inboundEventObjectMapper());
    }

    @Bean
    DeliveryKafkaListener deliveryKafkaListener(
            DeliveryInboundAdapter deliveryInboundAdapter) {
        return new DeliveryKafkaListener(deliveryInboundAdapter, inboundEventObjectMapper());
    }

    private static ObjectMapper inboundEventObjectMapper() {
        return JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
    }
}
