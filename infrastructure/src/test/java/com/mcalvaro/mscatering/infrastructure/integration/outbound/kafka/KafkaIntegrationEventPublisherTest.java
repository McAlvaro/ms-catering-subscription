package com.mcalvaro.mscatering.infrastructure.integration.outbound.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KafkaIntegrationEventPublisherTest {
    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);
    private final ObjectMapper mapper = JsonMapper.builder().addModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS).build();
    private KafkaIntegrationEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new KafkaIntegrationEventPublisher(kafkaTemplate, mapper,
                new KafkaIntegrationEventRouter("bc3.catering.eventos"), Duration.ofSeconds(5));
    }

    @Test
    void waitsForAcknowledgementAndSendsTheCompleteHeaderlessContractJson() throws Exception {
        when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

        publisher.publish(KafkaIntegrationEventFixtures.created());

        ArgumentCaptor<ProducerRecord<String, String>> records = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(kafkaTemplate).send(records.capture());
        ProducerRecord<String, String> record = records.getValue();
        assertThat(record.topic()).isEqualTo("bc3.catering.eventos");
        assertThat(record.key()).isEqualTo(KafkaIntegrationEventFixtures.SUBSCRIPTION_ID.toString());
        assertThat(record.headers()).isEmpty();
        JsonNode json = mapper.readTree(record.value());
        assertThat(json.has("eventId")).isTrue();
        assertThat(json.has("eventName")).isTrue();
        assertThat(json.has("eventVersion")).isTrue();
        assertThat(json.has("occurredOn")).isTrue();
        assertThat(json.has("source")).isTrue();
        assertThat(json.has("correlationId")).isTrue();
        assertThat(json.has("data")).isTrue();
        assertThat(json.has("logicalDestination")).isFalse();
        assertThat(json.get("eventId").asText()).isEqualTo(KafkaIntegrationEventFixtures.EVENT_ID.toString());
    }

    @Test
    void doesNotSendWhenSerializationFails() throws Exception {
        ObjectMapper failingMapper = mock(ObjectMapper.class);
        when(failingMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("bad json") { });
        KafkaIntegrationEventPublisher failingPublisher = new KafkaIntegrationEventPublisher(kafkaTemplate, failingMapper,
                new KafkaIntegrationEventRouter("bc3.catering.eventos"), Duration.ofSeconds(5));

        assertThatThrownBy(() -> failingPublisher.publish(KafkaIntegrationEventFixtures.created()))
                .isInstanceOf(KafkaIntegrationEventPublishException.class).hasCauseInstanceOf(JsonProcessingException.class);
        verify(kafkaTemplate, never()).send(any(ProducerRecord.class));
    }

    @Test
    void wrapsFailedAcknowledgementsWithTheirOriginalCause() {
        IllegalStateException cause = new IllegalStateException("broker rejected record");
        when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(CompletableFuture.failedFuture(cause));

        assertThatThrownBy(() -> publisher.publish(KafkaIntegrationEventFixtures.created()))
                .isInstanceOf(KafkaIntegrationEventPublishException.class).hasCause(cause);
    }

    @Test
    void propagatesSynchronousKafkaSendFailuresWithoutSilencingThem() {
        IllegalStateException failure = new IllegalStateException("producer unavailable");
        when(kafkaTemplate.send(any(ProducerRecord.class))).thenThrow(failure);

        assertThatThrownBy(() -> publisher.publish(KafkaIntegrationEventFixtures.created())).isSameAs(failure);
    }

    @Test
    void wrapsTimeoutAndRestoresTheInterruptedFlag() throws Exception {
        CompletableFuture<SendResult<String, String>> timeout = new CompletableFuture<>() {
            @Override public SendResult<String, String> get(long timeout, TimeUnit unit) throws TimeoutException {
                throw new TimeoutException("timed out");
            }
        };
        when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(timeout);
        assertThatThrownBy(() -> publisher.publish(KafkaIntegrationEventFixtures.created()))
                .isInstanceOf(KafkaIntegrationEventPublishException.class).hasCauseInstanceOf(TimeoutException.class);

        CompletableFuture<SendResult<String, String>> interrupted = new CompletableFuture<>() {
            @Override public SendResult<String, String> get(long timeout, TimeUnit unit) throws InterruptedException {
                throw new InterruptedException("interrupted");
            }
        };
        when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(interrupted);
        try {
            assertThatThrownBy(() -> publisher.publish(KafkaIntegrationEventFixtures.created()))
                    .isInstanceOf(KafkaIntegrationEventPublishException.class).hasCauseInstanceOf(InterruptedException.class);
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            Thread.interrupted();
        }
    }
}
