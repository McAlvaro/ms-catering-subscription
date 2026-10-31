package com.mcalvaro.mscatering.infrastructure.integration.inbound.bc5.delivery;

import an.awesome.pipelinr.Pipeline;
import com.mcalvaro.mscatering.application.subscription.ConfirmDelivery.ConfirmDeliveryCommand;
import com.mcalvaro.mscatering.application.subscription.RegisterFailedDelivery.RegisterFailedDeliveryCommand;
import com.mcalvaro.mscatering.domain.core.id.DeliveryDayId;
import com.mcalvaro.mscatering.domain.core.id.SubscriptionId;
import com.mcalvaro.mscatering.domain.subscription.ISubscriptionRepository;
import com.mcalvaro.mscatering.domain.subscription.Subscription;
import com.mcalvaro.mscatering.domain.subscription.entity.DeliveryCalendar;
import com.mcalvaro.mscatering.domain.subscription.entity.DeliveryDay;
import com.mcalvaro.mscatering.infrastructure.persistence.inboundevent.entity.ProcessedInboundEventReceiptJpaEntity;
import com.mcalvaro.mscatering.infrastructure.persistence.inboundevent.entity.ReceiptSubjectKind;
import com.mcalvaro.mscatering.infrastructure.persistence.inboundevent.repository.SpringDataProcessedInboundEventReceiptRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class DeliveryInboundAdapterTest {

    private Pipeline pipeline;
    private ISubscriptionRepository subscriptionRepository;
    private SpringDataProcessedInboundEventReceiptRepository receiptRepository;
    private DeliveryInboundAdapter adapter;

    private Subscription subscription;
    private DeliveryCalendar deliveryCalendar;
    private SubscriptionId subscriptionId;
    private DeliveryDayId dayId;
    private LocalDate deliveryDate;

    @BeforeEach
    void setUp() {
        pipeline = mock(Pipeline.class);
        subscriptionRepository = mock(ISubscriptionRepository.class);
        receiptRepository = mock(SpringDataProcessedInboundEventReceiptRepository.class);
        adapter = new DeliveryInboundAdapter(pipeline, subscriptionRepository, receiptRepository);

        subscriptionId = SubscriptionId.of("SUS-" + UUID.randomUUID());
        dayId = DeliveryDayId.of("CD-" + UUID.randomUUID());
        deliveryDate = LocalDate.of(2026, 10, 30);

        subscription = mock(Subscription.class);
        deliveryCalendar = mock(DeliveryCalendar.class);
        DeliveryDay deliveryDay = mock(DeliveryDay.class);

        when(subscription.getId()).thenReturn(subscriptionId);
        when(subscription.getDeliveryCalendar()).thenReturn(deliveryCalendar);
        when(deliveryDay.getId()).thenReturn(dayId);
        when(deliveryDay.getDate()).thenReturn(deliveryDate);

        when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(subscription));
    }

    @Nested
    @DisplayName("Direct Day Resolution")
    class DirectDayResolution {

        @Test
        @DisplayName("Resolves day directly when idCalendarioDia is present and dispatches ConfirmDeliveryCommand")
        void resolvesDayDirectlyAndDispatchesConfirmDelivery() {
            DeliveryDay day = mock(DeliveryDay.class);
            when(day.getId()).thenReturn(dayId);
            when(deliveryCalendar.findDayById(dayId)).thenReturn(Optional.of(day));

            UUID eventId = UUID.randomUUID();
            DeliveryConfirmedData data = new DeliveryConfirmedData(
                    "ENT-" + UUID.randomUUID(), "PKG-" + UUID.randomUUID(), dayId.value(), subscriptionId.value(), "PAC-" + UUID.randomUUID(),
                    "2026-10-30T14:30:00Z", "Juan Perez", "https://img.com/photo.png", null
            );
            DeliveryConfirmedEvent event = new DeliveryConfirmedEvent(
                    eventId, "bc5.EntregaConfirmada", "1.0", Instant.now(), "MsLogisticDelivery", "corr-101",
                    data
            );

            adapter.receiveConfirmed(event);

            ArgumentCaptor<ConfirmDeliveryCommand> cmdCaptor = ArgumentCaptor.forClass(ConfirmDeliveryCommand.class);
            verify(pipeline).send(cmdCaptor.capture());
            assertThat(cmdCaptor.getValue().subscriptionId()).isEqualTo(subscriptionId);
            assertThat(cmdCaptor.getValue().deliveryDayId()).isEqualTo(dayId);

            ArgumentCaptor<ProcessedInboundEventReceiptJpaEntity> receiptCaptor =
                    ArgumentCaptor.forClass(ProcessedInboundEventReceiptJpaEntity.class);
            verify(receiptRepository).saveAndFlush(receiptCaptor.capture());
            ProcessedInboundEventReceiptJpaEntity receipt = receiptCaptor.getValue();
            assertThat(receipt.getEventId()).isEqualTo(eventId);
            assertThat(receipt.getEventFingerprint()).isNotBlank();
            assertThat(receipt.getPayloadFingerprint()).isNotBlank();
        }

        @Test
        @DisplayName("Resolves day directly when idCalendarioDia is present and dispatches RegisterFailedDeliveryCommand")
        void resolvesDayDirectlyAndDispatchesRegisterFailedDelivery() {
            DeliveryDay day = mock(DeliveryDay.class);
            when(day.getId()).thenReturn(dayId);
            when(deliveryCalendar.findDayById(dayId)).thenReturn(Optional.of(day));

            UUID eventId = UUID.randomUUID();
            DeliveryFailedData data = new DeliveryFailedData(
                    "ENT-" + UUID.randomUUID(), "PKG-" + UUID.randomUUID(), dayId.value(), subscriptionId.value(), "PAC-" + UUID.randomUUID(),
                    deliveryDate, 3, "Paciente no respondio el timbre", false, null
            );
            DeliveryFailedEvent event = new DeliveryFailedEvent(
                    eventId, "bc5.EntregaNoConcretada", "1.0", Instant.now(), "MsLogisticDelivery", "corr-102",
                    data
            );

            adapter.receiveFailed(event);

            ArgumentCaptor<RegisterFailedDeliveryCommand> cmdCaptor = ArgumentCaptor.forClass(RegisterFailedDeliveryCommand.class);
            verify(pipeline).send(cmdCaptor.capture());
            assertThat(cmdCaptor.getValue().subscriptionId()).isEqualTo(subscriptionId);
            assertThat(cmdCaptor.getValue().deliveryDayId()).isEqualTo(dayId);
            assertThat(cmdCaptor.getValue().reason()).isEqualTo("Paciente no respondio el timbre");

            verify(receiptRepository).saveAndFlush(any());
        }
    }

    @Nested
    @DisplayName("Fallback Day Resolution by Date")
    class FallbackDateResolution {

        @Test
        @DisplayName("Falls back to fechaHoraEntrega when idCalendarioDia is null for Confirmed")
        void fallsBackToDateWhenIdCalendarioDiaIsNullConfirmed() {
            DeliveryDay day = mock(DeliveryDay.class);
            when(day.getId()).thenReturn(dayId);
            when(deliveryCalendar.getDaysForDate(LocalDate.of(2026, 10, 30))).thenReturn(List.of(day));

            DeliveryConfirmedData data = new DeliveryConfirmedData(
                    "ENT-" + UUID.randomUUID(), "PKG-" + UUID.randomUUID(), null, subscriptionId.value(), "PAC-" + UUID.randomUUID(),
                    "2026-10-30T10:15:00-04:00", "Maria Gomez", "https://img.com/evidence.png", null
            );
            DeliveryConfirmedEvent event = new DeliveryConfirmedEvent(
                    UUID.randomUUID(), "bc5.EntregaConfirmada", "1.0", Instant.now(), "MsLogisticDelivery", "corr-201",
                    data
            );

            adapter.receiveConfirmed(event);

            ArgumentCaptor<ConfirmDeliveryCommand> cmdCaptor = ArgumentCaptor.forClass(ConfirmDeliveryCommand.class);
            verify(pipeline).send(cmdCaptor.capture());
            assertThat(cmdCaptor.getValue().deliveryDayId()).isEqualTo(dayId);
            verify(receiptRepository).saveAndFlush(any());
        }

        @Test
        @DisplayName("Falls back to fechaEntrega when idCalendarioDia is null for Failed")
        void fallsBackToDateWhenIdCalendarioDiaIsNullFailed() {
            DeliveryDay day = mock(DeliveryDay.class);
            when(day.getId()).thenReturn(dayId);
            when(deliveryCalendar.getDaysForDate(deliveryDate)).thenReturn(List.of(day));

            DeliveryFailedData data = new DeliveryFailedData(
                    "ENT-" + UUID.randomUUID(), "PKG-" + UUID.randomUUID(), null, subscriptionId.value(), "PAC-" + UUID.randomUUID(),
                    deliveryDate, 2, "Direccion inaccesible por obras", true, "REPROGRAMACION"
            );
            DeliveryFailedEvent event = new DeliveryFailedEvent(
                    UUID.randomUUID(), "bc5.EntregaNoConcretada", "1.0", Instant.now(), "MsLogisticDelivery", "corr-202",
                    data
            );

            adapter.receiveFailed(event);

            ArgumentCaptor<RegisterFailedDeliveryCommand> cmdCaptor = ArgumentCaptor.forClass(RegisterFailedDeliveryCommand.class);
            verify(pipeline).send(cmdCaptor.capture());
            assertThat(cmdCaptor.getValue().deliveryDayId()).isEqualTo(dayId);
            assertThat(cmdCaptor.getValue().reason()).isEqualTo("Direccion inaccesible por obras");
            verify(receiptRepository).saveAndFlush(any());
        }

        @Test
        @DisplayName("Falls back to date when explicit idCalendarioDia is not found in calendar")
        void fallsBackToDateWhenDayIdNotFoundInCalendar() {
            DeliveryDayId unknownDayId = DeliveryDayId.of("CD-" + UUID.randomUUID());
            when(deliveryCalendar.findDayById(unknownDayId)).thenReturn(Optional.empty());

            DeliveryDay day = mock(DeliveryDay.class);
            when(day.getId()).thenReturn(dayId);
            when(deliveryCalendar.getDaysForDate(deliveryDate)).thenReturn(List.of(day));

            DeliveryConfirmedData data = new DeliveryConfirmedData(
                    "ENT-" + UUID.randomUUID(), "PKG-" + UUID.randomUUID(), unknownDayId.value(), subscriptionId.value(), "PAC-" + UUID.randomUUID(),
                    "2026-10-30T18:00:00Z", "John Doe", null, null
            );
            DeliveryConfirmedEvent event = new DeliveryConfirmedEvent(
                    UUID.randomUUID(), "bc5.EntregaConfirmada", "1.0", Instant.now(), "MsLogisticDelivery", "corr-203",
                    data
            );

            adapter.receiveConfirmed(event);

            ArgumentCaptor<ConfirmDeliveryCommand> cmdCaptor = ArgumentCaptor.forClass(ConfirmDeliveryCommand.class);
            verify(pipeline).send(cmdCaptor.capture());
            assertThat(cmdCaptor.getValue().deliveryDayId()).isEqualTo(dayId);
        }

        @Test
        @DisplayName("Falls back to date when explicit idCalendarioDia is not found in calendar for Failed")
        void fallsBackToDateWhenExplicitDayIdNotFoundInCalendarForFailed() {
            DeliveryDayId unknownDayId = DeliveryDayId.of("CD-" + UUID.randomUUID());
            when(deliveryCalendar.findDayById(unknownDayId)).thenReturn(Optional.empty());

            DeliveryDay day = mock(DeliveryDay.class);
            when(day.getId()).thenReturn(dayId);
            when(deliveryCalendar.getDaysForDate(deliveryDate)).thenReturn(List.of(day));

            DeliveryFailedData data = new DeliveryFailedData(
                    "ENT-" + UUID.randomUUID(), "PKG-" + UUID.randomUUID(), unknownDayId.value(), subscriptionId.value(), "PAC-" + UUID.randomUUID(),
                    deliveryDate, 2, "Direccion inaccesible", false, null
            );
            DeliveryFailedEvent event = new DeliveryFailedEvent(
                    UUID.randomUUID(), "bc5.EntregaNoConcretada", "1.0", Instant.now(), "MsLogisticDelivery", "corr-203-b",
                    data
            );

            adapter.receiveFailed(event);

            ArgumentCaptor<RegisterFailedDeliveryCommand> cmdCaptor = ArgumentCaptor.forClass(RegisterFailedDeliveryCommand.class);
            verify(pipeline).send(cmdCaptor.capture());
            assertThat(cmdCaptor.getValue().deliveryDayId()).isEqualTo(dayId);
        }

        @Test
        @DisplayName("Throws DEL-001 when day cannot be resolved by either ID or Date")
        void throwsDel001WhenDayCannotBeResolved() {
            when(deliveryCalendar.findDayById(any())).thenReturn(Optional.empty());
            when(deliveryCalendar.getDaysForDate(any())).thenReturn(List.of());

            DeliveryConfirmedData data = new DeliveryConfirmedData(
                    "ENT-" + UUID.randomUUID(), "PKG-" + UUID.randomUUID(), "CD-" + UUID.randomUUID(), subscriptionId.value(), "PAC-" + UUID.randomUUID(),
                    "2026-10-30T10:00:00Z", "John Doe", null, null
            );
            DeliveryConfirmedEvent event = new DeliveryConfirmedEvent(
                    UUID.randomUUID(), "bc5.EntregaConfirmada", "1.0", Instant.now(), "MsLogisticDelivery", "corr-204",
                    data
            );

            assertThatThrownBy(() -> adapter.receiveConfirmed(event))
                    .isInstanceOf(DeliveryContractException.class)
                    .hasFieldOrPropertyWithValue("code", "DEL-001");

            verifyNoInteractions(pipeline);
            verify(receiptRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("Throws DEL-001 when subscription does not exist")
        void throwsDel001WhenSubscriptionDoesNotExist() {
            SubscriptionId unknownSubId = SubscriptionId.of("SUS-" + UUID.randomUUID());
            when(subscriptionRepository.findById(unknownSubId)).thenReturn(Optional.empty());

            DeliveryConfirmedData data = new DeliveryConfirmedData(
                    "ENT-" + UUID.randomUUID(), "PKG-" + UUID.randomUUID(), dayId.value(), unknownSubId.value(), "PAC-" + UUID.randomUUID(),
                    "2026-10-30T10:00:00Z", "John Doe", null, null
            );
            DeliveryConfirmedEvent event = new DeliveryConfirmedEvent(
                    UUID.randomUUID(), "bc5.EntregaConfirmada", "1.0", Instant.now(), "MsLogisticDelivery", "corr-205",
                    data
            );

            assertThatThrownBy(() -> adapter.receiveConfirmed(event))
                    .isInstanceOf(DeliveryContractException.class)
                    .hasFieldOrPropertyWithValue("code", "DEL-001");

            verifyNoInteractions(pipeline);
            verify(receiptRepository, never()).saveAndFlush(any());
        }
    }

    @Nested
    @DisplayName("Idempotency and Deduplication")
    class IdempotencyAndDeduplication {

        @Test
        @DisplayName("Returns early without dispatching when eventId already processed with same fingerprint")
        void returnsEarlyWhenEventIdAlreadyProcessedWithSameFingerprint() {
            DeliveryDay day = mock(DeliveryDay.class);
            when(day.getId()).thenReturn(dayId);
            when(deliveryCalendar.findDayById(dayId)).thenReturn(Optional.of(day));

            UUID eventId = UUID.randomUUID();
            Instant occurredOn = Instant.parse("2026-10-30T10:00:00Z");
            DeliveryConfirmedData data = new DeliveryConfirmedData(
                    "ENT-" + UUID.randomUUID(), "PKG-" + UUID.randomUUID(), dayId.value(), subscriptionId.value(), "PAC-" + UUID.randomUUID(),
                    "2026-10-30T10:00:00Z", "John Doe", null, null
            );
            DeliveryConfirmedEvent event = new DeliveryConfirmedEvent(
                    eventId, "bc5.EntregaConfirmada", "1.0", occurredOn, "MsLogisticDelivery", "corr-301",
                    data
            );

            ProcessedInboundEventReceiptJpaEntity existingReceipt = mock(ProcessedInboundEventReceiptJpaEntity.class);
            when(receiptRepository.findById(eventId)).thenReturn(Optional.of(existingReceipt));

            String payloadFp = adapterSha256(
                    data.idEntrega(),
                    data.idPaquete(),
                    data.idCalendarioDia(),
                    data.subscriptionId(),
                    dayId.value(),
                    data.fechaHoraEntrega()
            );
            String eventFp = adapterSha256(
                    event.eventName(), event.eventVersion(), occurredOn.toString(),
                    event.source(), event.correlationId(), payloadFp
            );
            when(existingReceipt.getEventFingerprint()).thenReturn(eventFp);

            adapter.receiveConfirmed(event);

            verifyNoInteractions(pipeline);
            verify(receiptRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("Throws DEL-002 conflict when eventId reused with different fingerprint")
        void throwsConflictWhenEventIdReusedWithDifferentFingerprint() {
            DeliveryDay day = mock(DeliveryDay.class);
            when(day.getId()).thenReturn(dayId);
            when(deliveryCalendar.findDayById(dayId)).thenReturn(Optional.of(day));

            UUID eventId = UUID.randomUUID();
            DeliveryConfirmedData data = new DeliveryConfirmedData(
                    "ENT-" + UUID.randomUUID(), "PKG-" + UUID.randomUUID(), dayId.value(), subscriptionId.value(), "PAC-" + UUID.randomUUID(),
                    "2026-10-30T10:00:00Z", "John Doe", null, null
            );
            DeliveryConfirmedEvent event = new DeliveryConfirmedEvent(
                    eventId, "bc5.EntregaConfirmada", "1.0", Instant.now(), "MsLogisticDelivery", "corr-302",
                    data
            );

            ProcessedInboundEventReceiptJpaEntity existingReceipt = mock(ProcessedInboundEventReceiptJpaEntity.class);
            when(receiptRepository.findById(eventId)).thenReturn(Optional.of(existingReceipt));
            when(existingReceipt.getEventFingerprint()).thenReturn("different-fingerprint-hex");

            assertThatThrownBy(() -> adapter.receiveConfirmed(event))
                    .isInstanceOf(DeliveryContractException.class)
                    .hasFieldOrPropertyWithValue("code", "DEL-002");

            verifyNoInteractions(pipeline);
            verify(receiptRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("Returns early without dispatching when delivery day already processed with matching payload fingerprint")
        void returnsEarlyWhenDeliveryDayAlreadyProcessedWithMatchingPayload() {
            DeliveryDay day = mock(DeliveryDay.class);
            when(day.getId()).thenReturn(dayId);
            when(deliveryCalendar.findDayById(dayId)).thenReturn(Optional.of(day));

            UUID eventId = UUID.randomUUID();
            DeliveryConfirmedData data = new DeliveryConfirmedData(
                    "ENT-" + UUID.randomUUID(), "PKG-" + UUID.randomUUID(), dayId.value(), subscriptionId.value(), "PAC-" + UUID.randomUUID(),
                    "2026-10-30T10:00:00Z", "John Doe", null, null
            );
            DeliveryConfirmedEvent event = new DeliveryConfirmedEvent(
                    eventId, "bc5.EntregaConfirmada", "1.0", Instant.now(), "MsLogisticDelivery", "corr-303",
                    data
            );

            when(receiptRepository.findById(eventId)).thenReturn(Optional.empty());
            when(receiptRepository.existsBySubjectKindAndSubjectIdAndPayloadFingerprint(
                    eq(ReceiptSubjectKind.DELIVERY.name()), eq(dayId.value()), any()
            )).thenReturn(true);

            adapter.receiveConfirmed(event);

            verifyNoInteractions(pipeline);
            verify(receiptRepository, never()).saveAndFlush(any());
        }
    }

    private static String adapterSha256(String... values) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            for (String value : values) {
                byte[] bytes = (value != null ? value : "").getBytes(java.nio.charset.StandardCharsets.UTF_8);
                digest.update((byte) (bytes.length >>> 24));
                digest.update((byte) (bytes.length >>> 16));
                digest.update((byte) (bytes.length >>> 8));
                digest.update((byte) bytes.length);
                digest.update(bytes);
            }
            return java.util.HexFormat.of().formatHex(digest.digest());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
