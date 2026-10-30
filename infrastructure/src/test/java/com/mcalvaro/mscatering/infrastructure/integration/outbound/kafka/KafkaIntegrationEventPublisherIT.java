package com.mcalvaro.mscatering.infrastructure.integration.outbound.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mcalvaro.mscatering.application.integration.IntegrationEventPublisher;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "app.kafka.enabled=true",
        "outbox.relay-enabled=false",
        "outbox.provider=none",
        "spring.datasource.url=jdbc:h2:mem:kafka_integration_event_publisher;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE"
})
@ActiveProfiles("test")
class KafkaIntegrationEventPublisherIT {
    private static final String TOPIC = "bc3.catering.eventos";

    @Container
    static final KafkaContainer KAFKA = new KafkaContainer(DockerImageName.parse("apache/kafka-native:3.8.0"));

    @Autowired
    private IntegrationEventPublisher publisher;

    private final ObjectMapper objectMapper = JsonMapper.builder().addModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS).build();

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
    }

    @Test
    void publishesOnlyToTheConfiguredTopicWithAggregateKeysAndNoCustomHeaders() throws Exception {
        try (KafkaConsumer<String, String> consumer = consumer()) {
            consumer.subscribe(List.of(TOPIC));
            consumer.poll(Duration.ofMillis(200));

            publisher.publish(KafkaIntegrationEventFixtures.created());
            publisher.publish(KafkaIntegrationEventFixtures.paused());
            publisher.publish(KafkaIntegrationEventFixtures.calendarClosed());

            List<ConsumerRecord<String, String>> records = records(consumer, 3);
            assertThat(records).hasSize(3).allSatisfy(record -> assertThat(record.topic()).isEqualTo(TOPIC));

            List<ConsumerRecord<String, String>> subscriptionRecords = records.stream()
                    .filter(record -> record.key().equals(KafkaIntegrationEventFixtures.SUBSCRIPTION_ID.toString())).toList();
            assertThat(subscriptionRecords).extracting(ConsumerRecord::key)
                    .containsOnly(KafkaIntegrationEventFixtures.SUBSCRIPTION_ID.toString());
            assertThat(subscriptionRecords).extracting(record -> eventName(record.value()))
                    .containsExactly("bc3.SuscripcionCreada", "bc3.SuscripcionPausada");

            ConsumerRecord<String, String> calendarRecord = records.stream()
                    .filter(record -> record.key().equals(KafkaIntegrationEventFixtures.CALENDAR_ID.toString())).findFirst().orElseThrow();
            assertThat(calendarRecord.key()).isEqualTo(KafkaIntegrationEventFixtures.CALENDAR_ID.toString());
            assertThat(eventName(calendarRecord.value())).isEqualTo("bc3.CalendarioConsolidadoCerrado");
            assertThat(records).allSatisfy(record -> {
                assertThat(record.headers().lastHeader("eventName")).isNull();
                assertThat(eventId(record.value())).isEqualTo(KafkaIntegrationEventFixtures.EVENT_ID.toString());
            });
        }

        try (AdminClient admin = AdminClient.create(Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers()))) {
            assertThat(admin.listTopics().names().get()).containsExactly(TOPIC);
        }
    }

    private KafkaConsumer<String, String> consumer() {
        return new KafkaConsumer<>(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "kafka-integration-event-publisher-it-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class));
    }

    private static List<ConsumerRecord<String, String>> records(KafkaConsumer<String, String> consumer, int expected) {
        List<ConsumerRecord<String, String>> received = new ArrayList<>();
        long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
        while (received.size() < expected && System.nanoTime() < deadline) {
            ConsumerRecords<String, String> polled = consumer.poll(Duration.ofMillis(500));
            polled.forEach(received::add);
        }
        return received;
    }

    private String eventName(String json) {
        return json(json).get("eventName").asText();
    }

    private String eventId(String json) {
        return json(json).get("eventId").asText();
    }

    private JsonNode json(String value) {
        try {
            return objectMapper.readTree(value);
        } catch (Exception exception) {
            throw new AssertionError("Kafka record was not valid JSON", exception);
        }
    }
}
