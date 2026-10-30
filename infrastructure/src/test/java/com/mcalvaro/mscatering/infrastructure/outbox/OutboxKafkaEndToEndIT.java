package com.mcalvaro.mscatering.infrastructure.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mcalvaro.mscatering.application.subscription.CreateSubscription.CreateSubscriptionCommand;
import com.mcalvaro.mscatering.infrastructure.integration.inbound.bc1.address.PatientAddressRegistrationInboundAdapter;
import com.mcalvaro.mscatering.infrastructure.integration.inbound.bc1.patient.PatientRegistrationInboundAdapter;
import com.mcalvaro.mscatering.infrastructure.integration.inbound.bc2.dietplan.DietPlanInboundAdapter;
import com.mcalvaro.mscatering.infrastructure.persistence.outbox.entity.OutboxMessageJpaEntity;
import com.mcalvaro.mscatering.infrastructure.persistence.outbox.repository.SpringDataOutboxMessageRepository;
import com.mcalvaro.mscatering.infrastructure.persistence.patient.repository.SpringDataPatientReferenceRepository;
import com.mcalvaro.mscatering.infrastructure.support.DietPlanProjectionFixture;
import com.mcalvaro.mscatering.infrastructure.support.PatientAddressRegistrationFixture;
import com.mcalvaro.mscatering.infrastructure.support.PatientRegistrationFixture;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that a domain event persisted in the outbox is relayed to the real
 * Kafka topic. Uses the project's H2 schema and the production relay.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "app.kafka.enabled=true",
        "outbox.relay-enabled=true",
        "outbox.provider=kafka",
        "outbox.poll-interval=200ms",
        "outbox.batch-size=5",
        "spring.datasource.url=jdbc:h2:mem:outbox_kafka_e2e;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=" })
@ActiveProfiles("test")
class OutboxKafkaEndToEndIT {
    private static final String TOPIC = "bc3.catering.eventos";
    private static final Duration TIMEOUT = Duration.ofSeconds(60);

    @Container
    static final KafkaContainer KAFKA = new KafkaContainer(DockerImageName.parse("apache/kafka-native:3.8.0"));

    @LocalServerPort
    private int port;

    private final ObjectMapper objectMapper = JsonMapper.builder().addModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS).build();

    @Autowired
    private SpringDataOutboxMessageRepository outboxRepository;

    @Autowired
    private PatientRegistrationInboundAdapter patientInboundAdapter;

    @Autowired
    private SpringDataPatientReferenceRepository patientReferenceRepository;

    @Autowired
    private PatientAddressRegistrationInboundAdapter addressInboundAdapter;

    @Autowired
    private DietPlanInboundAdapter dietPlanInboundAdapter;

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
    }

    @Test
    void publishesThePersistedDomainEventToKafka() {
        UUID subscriptionId = createSubscription();

        await("the outbox message is processed", () -> outboxRows().stream()
                .anyMatch(OutboxMessageJpaEntity::isProcessed));

        try (KafkaConsumer<String, String> consumer = consumer()) {
            consumer.subscribe(List.of(TOPIC));
            List<ConsumerRecord<String, String>> records = records(consumer, 1);

            assertThat(records).hasSize(1);
            assertThat(records.get(0).topic()).isEqualTo(TOPIC);
            assertThat(records.get(0).key()).isEqualTo(subscriptionId.toString());
            assertThat(records.get(0).value()).contains("\"eventName\":\"bc3.SuscripcionCreada\"");
        }
    }

    private UUID createSubscription() {
        PatientRegistrationFixture patients = new PatientRegistrationFixture(patientInboundAdapter,
                patientReferenceRepository);
        PatientAddressRegistrationFixture addresses = new PatientAddressRegistrationFixture(addressInboundAdapter);
        DietPlanProjectionFixture plans = new DietPlanProjectionFixture(dietPlanInboundAdapter);

        UUID patientId = UUID.randomUUID();
        UUID addressId;
        LocalDate start = LocalDate.now().plusDays(1);
        UUID planId;
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
        try {
            patients.ingestEligiblePatient(patientId);
            addressId = addresses.ingestAddress(patientId);
            planId = plans.ingestActiveBasePlan(start, 15);
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }

        CreateSubscriptionCommand command = new CreateSubscriptionCommand(patientId, planId, start,
                start.plusDays(14), "LUNCH", BigDecimal.TEN, "accepted", addressId, LocalTime.of(12, 0),
                LocalTime.of(14, 0), "no onions");
        return postSubscription(command);
    }

    private UUID postSubscription(CreateSubscriptionCommand command) {
        try {
            HttpRequest request = HttpRequest
                    .newBuilder(URI.create("http://localhost:" + port + "/api/subscriptions"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(command)))
                    .build();
            HttpResponse<String> response = HttpClient.newHttpClient().send(request,
                    HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).as("HTTP body: %s", response.body()).isEqualTo(201);
            return UUID.fromString(response.body().replace("\"", ""));
        } catch (IOException exception) {
            throw new IllegalStateException("Could not create the subscription through the HTTP API", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while creating the subscription", exception);
        }
    }

    private List<OutboxMessageJpaEntity> outboxRows() {
        return outboxRepository.findAll().stream()
                .filter(row -> "subscription.created".equals(row.getType())).toList();
    }

    private static void await(String description, BooleanSupplier condition) {
        long deadline = System.nanoTime() + TIMEOUT.toNanos();
        while (System.nanoTime() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting for " + description, exception);
            }
        }
        throw new AssertionError("Timed out waiting for " + description);
    }

    private KafkaConsumer<String, String> consumer() {
        Map<String, Object> properties = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "outbox-e2e-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return new KafkaConsumer<>(properties);
    }

    private static List<ConsumerRecord<String, String>> records(KafkaConsumer<String, String> consumer,
            int expected) {
        List<ConsumerRecord<String, String>> received = new ArrayList<>();
        long deadline = System.nanoTime() + TIMEOUT.toNanos();
        while (received.size() < expected && System.nanoTime() < deadline) {
            ConsumerRecords<String, String> polled = consumer.poll(Duration.ofMillis(500));
            polled.forEach(received::add);
        }
        return received;
    }
}
