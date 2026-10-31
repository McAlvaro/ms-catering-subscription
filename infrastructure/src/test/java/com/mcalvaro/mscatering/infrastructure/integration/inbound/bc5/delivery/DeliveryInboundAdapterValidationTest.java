package com.mcalvaro.mscatering.infrastructure.integration.inbound.bc5.delivery;

import an.awesome.pipelinr.Pipeline;
import com.mcalvaro.mscatering.domain.subscription.ISubscriptionRepository;
import com.mcalvaro.mscatering.infrastructure.persistence.inboundevent.repository.SpringDataProcessedInboundEventReceiptRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class DeliveryInboundAdapterValidationTest {

    private Pipeline pipeline;
    private ISubscriptionRepository subscriptionRepository;
    private SpringDataProcessedInboundEventReceiptRepository receiptRepository;
    private DeliveryInboundAdapter adapter;

    @BeforeEach
    void setUp() {
        pipeline = mock(Pipeline.class);
        subscriptionRepository = mock(ISubscriptionRepository.class);
        receiptRepository = mock(SpringDataProcessedInboundEventReceiptRepository.class);
        adapter = new DeliveryInboundAdapter(pipeline, subscriptionRepository, receiptRepository);
    }

    @Nested
    @DisplayName("DeliveryConfirmedEvent Envelope Validation")
    class ConfirmedEnvelopeValidation {

        @Test
        @DisplayName("Rejects null event")
        void rejectsNullEvent() {
            assertThatThrownBy(() -> adapter.receiveConfirmed(null))
                    .isInstanceOf(DeliveryContractException.class)
                    .hasFieldOrPropertyWithValue("code", "PROJ-001");
        }

        @Test
        @DisplayName("Rejects null eventId")
        void rejectsNullEventId() {
            DeliveryConfirmedEvent event = new DeliveryConfirmedEvent(
                    null, "bc5.EntregaConfirmada", "1.0", Instant.now(), "MsLogisticDelivery", "corr-1",
                    validConfirmedData()
            );
            assertThatThrownBy(() -> adapter.receiveConfirmed(event))
                    .isInstanceOf(DeliveryContractException.class)
                    .hasFieldOrPropertyWithValue("code", "PROJ-001");
        }

        @Test
        @DisplayName("Rejects invalid eventName")
        void rejectsInvalidEventName() {
            DeliveryConfirmedEvent event = new DeliveryConfirmedEvent(
                    UUID.randomUUID(), "bc5.WrongEvent", "1.0", Instant.now(), "MsLogisticDelivery", "corr-1",
                    validConfirmedData()
            );
            assertThatThrownBy(() -> adapter.receiveConfirmed(event))
                    .isInstanceOf(DeliveryContractException.class)
                    .hasFieldOrPropertyWithValue("code", "PROJ-001");
        }

        @Test
        @DisplayName("Rejects invalid eventVersion")
        void rejectsInvalidEventVersion() {
            DeliveryConfirmedEvent event = new DeliveryConfirmedEvent(
                    UUID.randomUUID(), "bc5.EntregaConfirmada", "2.0", Instant.now(), "MsLogisticDelivery", "corr-1",
                    validConfirmedData()
            );
            assertThatThrownBy(() -> adapter.receiveConfirmed(event))
                    .isInstanceOf(DeliveryContractException.class)
                    .hasFieldOrPropertyWithValue("code", "PROJ-001");
        }

        @Test
        @DisplayName("Rejects null occurredOn")
        void rejectsNullOccurredOn() {
            DeliveryConfirmedEvent event = new DeliveryConfirmedEvent(
                    UUID.randomUUID(), "bc5.EntregaConfirmada", "1.0", null, "MsLogisticDelivery", "corr-1",
                    validConfirmedData()
            );
            assertThatThrownBy(() -> adapter.receiveConfirmed(event))
                    .isInstanceOf(DeliveryContractException.class)
                    .hasFieldOrPropertyWithValue("code", "PROJ-001");
        }

        @Test
        @DisplayName("Rejects invalid source")
        void rejectsInvalidSource() {
            DeliveryConfirmedEvent event = new DeliveryConfirmedEvent(
                    UUID.randomUUID(), "bc5.EntregaConfirmada", "1.0", Instant.now(), "UnknownSource", "corr-1",
                    validConfirmedData()
            );
            assertThatThrownBy(() -> adapter.receiveConfirmed(event))
                    .isInstanceOf(DeliveryContractException.class)
                    .hasFieldOrPropertyWithValue("code", "PROJ-001");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"  ", "\t\n"})
        @DisplayName("Rejects blank or null correlationId")
        void rejectsBlankCorrelationId(String correlationId) {
            DeliveryConfirmedEvent event = new DeliveryConfirmedEvent(
                    UUID.randomUUID(), "bc5.EntregaConfirmada", "1.0", Instant.now(), "MsLogisticDelivery", correlationId,
                    validConfirmedData()
            );
            assertThatThrownBy(() -> adapter.receiveConfirmed(event))
                    .isInstanceOf(DeliveryContractException.class)
                    .hasFieldOrPropertyWithValue("code", "PROJ-001");
        }

        @Test
        @DisplayName("Rejects null data")
        void rejectsNullData() {
            DeliveryConfirmedEvent event = new DeliveryConfirmedEvent(
                    UUID.randomUUID(), "bc5.EntregaConfirmada", "1.0", Instant.now(), "MsLogisticDelivery", "corr-1",
                    null
            );
            assertThatThrownBy(() -> adapter.receiveConfirmed(event))
                    .isInstanceOf(DeliveryContractException.class)
                    .hasFieldOrPropertyWithValue("code", "PROJ-001");
        }

        @Test
        @DisplayName("Rejects missing idEntrega")
        void rejectsMissingIdEntrega() {
            DeliveryConfirmedData data = new DeliveryConfirmedData(
                    null, "PKG-1", "CD-1", "SUS-1", "PAC-1",
                    "2026-10-30T10:00:00Z", "John Doe", "https://photos.com/1", null
            );
            DeliveryConfirmedEvent event = confirmedEvent(data);
            assertThatThrownBy(() -> adapter.receiveConfirmed(event))
                    .isInstanceOf(DeliveryContractException.class)
                    .hasFieldOrPropertyWithValue("code", "PROJ-001");
        }

        @Test
        @DisplayName("Rejects missing idSuscripcion")
        void rejectsMissingIdSuscripcion() {
            DeliveryConfirmedData data = new DeliveryConfirmedData(
                    "ENT-1", "PKG-1", "CD-1", null, "PAC-1",
                    "2026-10-30T10:00:00Z", "John Doe", "https://photos.com/1", null
            );
            DeliveryConfirmedEvent event = confirmedEvent(data);
            assertThatThrownBy(() -> adapter.receiveConfirmed(event))
                    .isInstanceOf(DeliveryContractException.class)
                    .hasFieldOrPropertyWithValue("code", "PROJ-001");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"  ", "invalid-date"})
        @DisplayName("Rejects invalid or blank fechaHoraEntrega")
        void rejectsInvalidFechaHoraEntrega(String fechaHoraEntrega) {
            DeliveryConfirmedData data = new DeliveryConfirmedData(
                    "ENT-1", "PKG-1", "CD-1", "SUS-1", "PAC-1",
                    fechaHoraEntrega, "John Doe", "https://photos.com/1", null
            );
            DeliveryConfirmedEvent event = confirmedEvent(data);
            assertThatThrownBy(() -> adapter.receiveConfirmed(event))
                    .isInstanceOf(DeliveryContractException.class)
                    .hasFieldOrPropertyWithValue("code", "PROJ-001");
        }
    }

    @Nested
    @DisplayName("DeliveryFailedEvent Envelope Validation")
    class FailedEnvelopeValidation {

        @Test
        @DisplayName("Rejects null event")
        void rejectsNullEvent() {
            assertThatThrownBy(() -> adapter.receiveFailed(null))
                    .isInstanceOf(DeliveryContractException.class)
                    .hasFieldOrPropertyWithValue("code", "PROJ-001");
        }

        @Test
        @DisplayName("Rejects invalid eventName for failed event")
        void rejectsInvalidEventName() {
            DeliveryFailedEvent event = new DeliveryFailedEvent(
                    UUID.randomUUID(), "bc5.WrongEvent", "1.0", Instant.now(), "MsLogisticDelivery", "corr-1",
                    validFailedData()
            );
            assertThatThrownBy(() -> adapter.receiveFailed(event))
                    .isInstanceOf(DeliveryContractException.class)
                    .hasFieldOrPropertyWithValue("code", "PROJ-001");
        }

        @Test
        @DisplayName("Rejects null data")
        void rejectsNullData() {
            DeliveryFailedEvent event = new DeliveryFailedEvent(
                    UUID.randomUUID(), "bc5.EntregaNoConcretada", "1.0", Instant.now(), "MsLogisticDelivery", "corr-1",
                    null
            );
            assertThatThrownBy(() -> adapter.receiveFailed(event))
                    .isInstanceOf(DeliveryContractException.class)
                    .hasFieldOrPropertyWithValue("code", "PROJ-001");
        }

        @Test
        @DisplayName("Rejects missing idEntrega")
        void rejectsMissingIdEntrega() {
            DeliveryFailedData data = new DeliveryFailedData(
                    null, "PKG-1", "CD-1", "SUS-1", "PAC-1",
                    LocalDate.of(2026, 10, 30), 2, "Direccion no encontrada", false, null
            );
            DeliveryFailedEvent event = failedEvent(data);
            assertThatThrownBy(() -> adapter.receiveFailed(event))
                    .isInstanceOf(DeliveryContractException.class)
                    .hasFieldOrPropertyWithValue("code", "PROJ-001");
        }

        @Test
        @DisplayName("Rejects missing idSuscripcion")
        void rejectsMissingIdSuscripcion() {
            DeliveryFailedData data = new DeliveryFailedData(
                    "ENT-1", "PKG-1", "CD-1", null, "PAC-1",
                    LocalDate.of(2026, 10, 30), 2, "Direccion no encontrada", false, null
            );
            DeliveryFailedEvent event = failedEvent(data);
            assertThatThrownBy(() -> adapter.receiveFailed(event))
                    .isInstanceOf(DeliveryContractException.class)
                    .hasFieldOrPropertyWithValue("code", "PROJ-001");
        }

        @Test
        @DisplayName("Rejects null fechaEntrega")
        void rejectsNullFechaEntrega() {
            DeliveryFailedData data = new DeliveryFailedData(
                    "ENT-1", "PKG-1", "CD-1", "SUS-1", "PAC-1",
                    (LocalDate) null, 2, "Direccion no encontrada", false, null
            );
            DeliveryFailedEvent event = failedEvent(data);
            assertThatThrownBy(() -> adapter.receiveFailed(event))
                    .isInstanceOf(DeliveryContractException.class)
                    .hasFieldOrPropertyWithValue("code", "PROJ-001");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"  ", "\t\n"})
        @DisplayName("Rejects blank or null motivoFinal")
        void rejectsBlankMotivoFinal(String motivo) {
            DeliveryFailedData data = new DeliveryFailedData(
                    "ENT-1", "PKG-1", "CD-1", "SUS-1", "PAC-1",
                    LocalDate.of(2026, 10, 30), 2, motivo, false, null
            );
            DeliveryFailedEvent event = failedEvent(data);
            assertThatThrownBy(() -> adapter.receiveFailed(event))
                    .isInstanceOf(DeliveryContractException.class)
                    .hasFieldOrPropertyWithValue("code", "PROJ-001");
        }
    }

    private DeliveryConfirmedData validConfirmedData() {
        return new DeliveryConfirmedData(
                "ENT-1", "PKG-1", "CD-1", "SUS-1", "PAC-1",
                "2026-10-30T10:00:00Z", "John Doe", "https://photos.com/1", null
        );
    }

    private DeliveryConfirmedEvent confirmedEvent(DeliveryConfirmedData data) {
        return new DeliveryConfirmedEvent(
                UUID.randomUUID(), "bc5.EntregaConfirmada", "1.0", Instant.now(), "MsLogisticDelivery", "corr-1",
                data
        );
    }

    private DeliveryFailedData validFailedData() {
        return new DeliveryFailedData(
                "ENT-1", "PKG-1", "CD-1", "SUS-1", "PAC-1",
                LocalDate.of(2026, 10, 30), 2, "Direccion no encontrada", false, null
        );
    }

    private DeliveryFailedEvent failedEvent(DeliveryFailedData data) {
        return new DeliveryFailedEvent(
                UUID.randomUUID(), "bc5.EntregaNoConcretada", "1.0", Instant.now(), "MsLogisticDelivery", "corr-1",
                data
        );
    }
}
