package com.mcalvaro.mscatering.infrastructure.integration.inbound.bc5.delivery;

import an.awesome.pipelinr.Pipeline;
import com.mcalvaro.mscatering.application.subscription.ConfirmDelivery.ConfirmDeliveryCommand;
import com.mcalvaro.mscatering.application.subscription.RegisterFailedDelivery.RegisterFailedDeliveryCommand;
import com.mcalvaro.mscatering.domain.subscription.ISubscriptionRepository;
import com.mcalvaro.mscatering.domain.subscription.Subscription;
import com.mcalvaro.mscatering.domain.subscription.entity.DeliveryDay;
import com.mcalvaro.mscatering.infrastructure.persistence.inboundevent.entity.ProcessedInboundEventReceiptJpaEntity;
import com.mcalvaro.mscatering.infrastructure.persistence.inboundevent.entity.ReceiptSubjectKind;
import com.mcalvaro.mscatering.infrastructure.persistence.inboundevent.repository.SpringDataProcessedInboundEventReceiptRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Transport-neutral inbound anti-corruption adapter for BC5 Logistics delivery integration events.
 */
@Component
public class DeliveryInboundAdapter {

    private static final Logger log = LoggerFactory.getLogger(DeliveryInboundAdapter.class);

    public static final String SOURCE = "MsLogisticDelivery";
    public static final String EVENT_VERSION = "1.0";
    public static final String CONFIRMED_EVENT = "bc5.EntregaConfirmada";
    public static final String FAILED_EVENT = "bc5.EntregaNoConcretada";

    private final Pipeline pipeline;
    private final ISubscriptionRepository subscriptionRepository;
    private final SpringDataProcessedInboundEventReceiptRepository receiptRepository;

    public DeliveryInboundAdapter(Pipeline pipeline,
                                  ISubscriptionRepository subscriptionRepository,
                                  SpringDataProcessedInboundEventReceiptRepository receiptRepository) {
        this.pipeline = Objects.requireNonNull(pipeline, "pipeline must not be null");
        this.subscriptionRepository = Objects.requireNonNull(subscriptionRepository, "subscriptionRepository must not be null");
        this.receiptRepository = Objects.requireNonNull(receiptRepository, "receiptRepository must not be null");
    }

    @Transactional
    public void receiveConfirmed(DeliveryConfirmedEvent event) {
        validateConfirmedEnvelope(event);

        DeliveryConfirmedData data = event.data();
        Subscription subscription = subscriptionRepository.findById(data.subscriptionId())
                .orElseThrow(() -> DeliveryContractException.deliveryDayNotFound(
                        "Subscription not found: " + data.subscriptionId()));

        LocalDate deliveryDate = parseConfirmedDate(data.fechaHoraEntrega());
        UUID resolvedDeliveryDayId = resolveDeliveryDayId(subscription, data.calendarDayId(), deliveryDate);

        String payloadFingerprint = confirmedPayloadFingerprint(data, resolvedDeliveryDayId);
        String eventFingerprint = eventFingerprint(event.eventName(), event.eventVersion(),
                event.occurredOn(), event.source(), event.correlationId(), payloadFingerprint);

        if (isAlreadyProcessed(event.eventId(), eventFingerprint, resolvedDeliveryDayId, payloadFingerprint)) {
            return;
        }

        pipeline.send(new ConfirmDeliveryCommand(data.subscriptionId(), resolvedDeliveryDayId));

        saveReceipt(event.eventId(), event.eventName(), event.eventVersion(), event.occurredOn(),
                event.source(), event.correlationId(), data.patientId(), resolvedDeliveryDayId,
                eventFingerprint, payloadFingerprint);
    }

    @Transactional
    public void receiveFailed(DeliveryFailedEvent event) {
        validateFailedEnvelope(event);

        DeliveryFailedData data = event.data();
        Subscription subscription = subscriptionRepository.findById(data.subscriptionId())
                .orElseThrow(() -> DeliveryContractException.deliveryDayNotFound(
                        "Subscription not found: " + data.subscriptionId()));

        UUID resolvedDeliveryDayId = resolveDeliveryDayId(subscription, data.calendarDayId(), data.fechaEntrega());

        String payloadFingerprint = failedPayloadFingerprint(data, resolvedDeliveryDayId);
        String eventFingerprint = eventFingerprint(event.eventName(), event.eventVersion(),
                event.occurredOn(), event.source(), event.correlationId(), payloadFingerprint);

        if (isAlreadyProcessed(event.eventId(), eventFingerprint, resolvedDeliveryDayId, payloadFingerprint)) {
            return;
        }

        pipeline.send(new RegisterFailedDeliveryCommand(data.subscriptionId(), resolvedDeliveryDayId, data.motivoFinal()));

        saveReceipt(event.eventId(), event.eventName(), event.eventVersion(), event.occurredOn(),
                event.source(), event.correlationId(), data.patientId(), resolvedDeliveryDayId,
                eventFingerprint, payloadFingerprint);
    }

    public void receive(DeliveryConfirmedEvent event) {
        receiveConfirmed(event);
    }

    public void receive(DeliveryFailedEvent event) {
        receiveFailed(event);
    }

    private void validateConfirmedEnvelope(DeliveryConfirmedEvent event) {
        if (event == null || event.eventId() == null || !CONFIRMED_EVENT.equals(event.eventName())
                || !EVENT_VERSION.equals(event.eventVersion()) || event.occurredOn() == null
                || !SOURCE.equals(event.source()) || isBlank(event.correlationId()) || event.data() == null) {
            throw DeliveryContractException.invalid("Invalid BC5 EntregaConfirmada event envelope");
        }

        DeliveryConfirmedData data = event.data();
        if (data.idEntrega() == null || data.idSuscripcion() == null || isBlank(data.fechaHoraEntrega())) {
            throw DeliveryContractException.invalid("Missing mandatory fields in DeliveryConfirmedData");
        }

        try {
            parseConfirmedDate(data.fechaHoraEntrega());
        } catch (Exception e) {
            throw DeliveryContractException.invalid("Invalid fechaHoraEntrega format: " + data.fechaHoraEntrega());
        }
    }

    private void validateFailedEnvelope(DeliveryFailedEvent event) {
        if (event == null || event.eventId() == null || !FAILED_EVENT.equals(event.eventName())
                || !EVENT_VERSION.equals(event.eventVersion()) || event.occurredOn() == null
                || !SOURCE.equals(event.source()) || isBlank(event.correlationId()) || event.data() == null) {
            throw DeliveryContractException.invalid("Invalid BC5 EntregaNoConcretada event envelope");
        }

        DeliveryFailedData data = event.data();
        if (data.idEntrega() == null || data.idSuscripcion() == null || data.fechaEntrega() == null
                || isBlank(data.motivoFinal())) {
            throw DeliveryContractException.invalid("Missing mandatory fields in DeliveryFailedData");
        }
    }

    private UUID resolveDeliveryDayId(Subscription subscription, UUID explicitDayId, LocalDate fallbackDate) {
        if (explicitDayId != null) {
            Optional<DeliveryDay> day = subscription.getDeliveryCalendar().findDayById(explicitDayId);
            if (day.isPresent()) {
                return day.get().getId();
            }
        }

        if (fallbackDate != null) {
            List<DeliveryDay> days = subscription.getDeliveryCalendar().getDaysForDate(fallbackDate);
            if (days != null && !days.isEmpty()) {
                return days.get(0).getId();
            }
        }

        throw DeliveryContractException.deliveryDayNotFound(
                "Delivery day could not be resolved in subscription " + subscription.getId()
                        + " for dayId: " + explicitDayId + " or date: " + fallbackDate);
    }

    private boolean isAlreadyProcessed(UUID eventId, String eventFingerprint, UUID deliveryDayId, String payloadFingerprint) {
        var existingReceipt = receiptRepository.findById(eventId);
        if (existingReceipt.isPresent()) {
            if (existingReceipt.get().getEventFingerprint().equals(eventFingerprint)) {
                log.info("Delivery event {} already processed with matching fingerprint; skipping deduplicated dispatch", eventId);
                return true;
            }
            throw DeliveryContractException.conflict("Inbound eventId was reused with different content: " + eventId);
        }

        if (receiptRepository.existsBySubjectKindAndSubjectIdAndPayloadFingerprint(
                ReceiptSubjectKind.DELIVERY.name(), deliveryDayId, payloadFingerprint)) {
            log.info("Delivery day {} already processed with matching payload; skipping dispatch", deliveryDayId);
            return true;
        }

        return false;
    }

    private void saveReceipt(UUID eventId, String eventName, String eventVersion, Instant occurredOn,
                             String source, String correlationId, UUID patientId, UUID deliveryDayId,
                             String eventFingerprint, String payloadFingerprint) {
        ProcessedInboundEventReceiptJpaEntity receipt = new ProcessedInboundEventReceiptJpaEntity(
                eventId,
                eventName,
                eventVersion,
                occurredOn,
                source,
                correlationId,
                ReceiptSubjectKind.DELIVERY,
                deliveryDayId,
                eventFingerprint,
                payloadFingerprint
        );
        receiptRepository.saveAndFlush(receipt);
    }

    private LocalDate parseConfirmedDate(String fechaHoraEntrega) {
        if (fechaHoraEntrega == null || fechaHoraEntrega.isBlank()) {
            throw new IllegalArgumentException("fechaHoraEntrega must not be blank");
        }
        try {
            return OffsetDateTime.parse(fechaHoraEntrega).toLocalDate();
        } catch (DateTimeParseException e1) {
            try {
                return Instant.parse(fechaHoraEntrega).atZone(ZoneOffset.UTC).toLocalDate();
            } catch (DateTimeParseException e2) {
                try {
                    return LocalDateTime.parse(fechaHoraEntrega).toLocalDate();
                } catch (DateTimeParseException e3) {
                    return LocalDate.parse(fechaHoraEntrega);
                }
            }
        }
    }

    private String confirmedPayloadFingerprint(DeliveryConfirmedData data, UUID resolvedDeliveryDayId) {
        return sha256(
                safeString(data.idEntrega()),
                safeString(data.idPaquete()),
                safeString(data.idCalendarioDia()),
                data.subscriptionId().toString(),
                resolvedDeliveryDayId.toString(),
                data.fechaHoraEntrega()
        );
    }

    private String failedPayloadFingerprint(DeliveryFailedData data, UUID resolvedDeliveryDayId) {
        return sha256(
                safeString(data.idEntrega()),
                safeString(data.idPaquete()),
                safeString(data.idCalendarioDia()),
                data.subscriptionId().toString(),
                resolvedDeliveryDayId.toString(),
                data.fechaEntrega().toString(),
                data.motivoFinal()
        );
    }

    private String eventFingerprint(String eventName, String eventVersion, Instant occurredOn,
                                    String source, String correlationId, String payloadFingerprint) {
        return sha256(eventName, eventVersion, occurredOn.toString(), source, correlationId, payloadFingerprint);
    }

    private static String safeString(Object value) {
        return value != null ? value.toString() : "";
    }

    private static String sha256(String... values) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (String value : values) {
                byte[] bytes = (value != null ? value : "").getBytes(StandardCharsets.UTF_8);
                digest.update((byte) (bytes.length >>> 24));
                digest.update((byte) (bytes.length >>> 16));
                digest.update((byte) (bytes.length >>> 8));
                digest.update((byte) bytes.length);
                digest.update(bytes);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private static boolean isBlank(String str) {
        return str == null || str.isBlank();
    }
}
