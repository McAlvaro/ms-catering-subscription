package com.mcalvaro.mscatering.infrastructure.integration.outbound.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mcalvaro.mscatering.application.integration.IntegrationEventPublisher;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Duration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "app.kafka", name = "enabled", havingValue = "true")
public class KafkaIntegrationConfiguration {
    private static final Duration ACKNOWLEDGEMENT_TIMEOUT = Duration.ofSeconds(5);

    @Bean
    KafkaIntegrationEventRouter kafkaIntegrationEventRouter(@Value("${app.kafka.topic:bc3.catering.eventos}") String topic) {
        return new KafkaIntegrationEventRouter(topic);
    }

    @Bean
    IntegrationEventPublisher kafkaIntegrationEventPublisher(KafkaTemplate<String, String> kafkaTemplate,
            KafkaIntegrationEventRouter router) {
        return new KafkaIntegrationEventPublisher(kafkaTemplate, integrationEventObjectMapper(), router,
                ACKNOWLEDGEMENT_TIMEOUT);
    }

    @Bean
    NewTopic cateringIntegrationEventsTopic(@Value("${app.kafka.topic:bc3.catering.eventos}") String topic) {
        return TopicBuilder.name(topic).partitions(3).replicas(1).build();
    }

    private static ObjectMapper integrationEventObjectMapper() {
        return JsonMapper.builder().addModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS).build();
    }
}
