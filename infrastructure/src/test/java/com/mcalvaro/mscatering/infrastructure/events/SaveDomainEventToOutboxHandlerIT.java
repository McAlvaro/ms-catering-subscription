package com.mcalvaro.mscatering.infrastructure.events;

import an.awesome.pipelinr.Pipeline;
import com.mcalvaro.mscatering.application.abstractions.DomainEventDispatcher;
import com.mcalvaro.mscatering.application.outbox.OutboxMessage;
import com.mcalvaro.mscatering.application.outbox.OutboxMessageRepository;
import com.mcalvaro.mscatering.application.subscription.ConfirmDelivery.ConfirmDeliveryCommand;
import com.mcalvaro.mscatering.application.subscription.CreateSubscription.CreateSubscriptionCommand;
import com.mcalvaro.mscatering.domain.core.DomainException;
import com.mcalvaro.mscatering.domain.core.id.AddressId;
import com.mcalvaro.mscatering.domain.core.id.DeliveryDayId;
import com.mcalvaro.mscatering.domain.core.id.DietPlanId;
import com.mcalvaro.mscatering.domain.core.id.PatientId;
import com.mcalvaro.mscatering.domain.core.id.SubscriptionId;
import com.mcalvaro.mscatering.domain.dietplan.PlanType;
import com.mcalvaro.mscatering.domain.subscription.ISubscriptionRepository;
import com.mcalvaro.mscatering.domain.subscription.Subscription;
import com.mcalvaro.mscatering.domain.subscription.enums.DeliveryDayStatus;
import com.mcalvaro.mscatering.domain.subscription.enums.ServiceType;
import com.mcalvaro.mscatering.domain.subscription.event.SubscriptionCreated;
import com.mcalvaro.mscatering.domain.subscription.vo.DeliveryAddress;
import com.mcalvaro.mscatering.domain.subscription.vo.DeliveryPreferences;
import com.mcalvaro.mscatering.domain.subscription.vo.ServiceContract;
import com.mcalvaro.mscatering.domain.subscription.vo.TimeWindow;
import com.mcalvaro.mscatering.domain.subscription.vo.ValidityPeriod;
import com.mcalvaro.mscatering.infrastructure.BaseIntegrationTest;
import com.mcalvaro.mscatering.infrastructure.persistence.outbox.codec.JacksonDomainEventCodec;
import com.mcalvaro.mscatering.infrastructure.persistence.outbox.entity.OutboxMessageJpaEntity;
import com.mcalvaro.mscatering.infrastructure.persistence.outbox.repository.SpringDataOutboxMessageRepository;
import com.mcalvaro.mscatering.infrastructure.persistence.subscription.repository.SpringDataSubscriptionRepository;
import com.mcalvaro.mscatering.infrastructure.support.DietPlanProjectionFixture;
import com.mcalvaro.mscatering.infrastructure.support.PatientAddressRegistrationFixture;
import com.mcalvaro.mscatering.infrastructure.support.PatientRegistrationFixture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.mock.web.MockHttpServletRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional(propagation = Propagation.NOT_SUPPORTED)
class SaveDomainEventToOutboxHandlerIT extends BaseIntegrationTest {

    @Autowired
    private Pipeline pipeline;

    @Autowired
    private ISubscriptionRepository subscriptionRepository;

    @Autowired
    private DomainEventDispatcher dispatcher;

    @Autowired
    private OutboxMessageRepository outboxMessageRepository;

    @Autowired
    private SpringDataOutboxMessageRepository outboxJpaRepository;

    @Autowired
    private SpringDataSubscriptionRepository subscriptionJpaRepository;

    @Autowired
    private JacksonDomainEventCodec codec;

    @Autowired
    private PatientRegistrationFixture patientFixture;

    @Autowired
    private PatientAddressRegistrationFixture addressFixture;

    @Autowired
    private DietPlanProjectionFixture dietPlanFixture;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void activateRequestScope() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
    }

    @AfterEach
    void clearRequestScope() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void persistsOneDecodedSubscriptionCreatedOutboxMessageInTheCreationTransaction() {
        CreatedSubscription created = createSubscriptionThroughPipeline();

        assertThat(subscriptionJpaRepository.findById(created.subscriptionId().value())).isPresent();

        List<OutboxMessageJpaEntity> rows = subscriptionCreatedRowsFor(created.subscriptionId());

        assertThat(rows).singleElement().satisfies(row -> {
            SubscriptionCreated event = (SubscriptionCreated) codec.decode(row.getType(), row.getContent());
            assertThat(row.getId()).isEqualTo(event.eventId());
            assertThat(event.subscriptionId()).isEqualTo(created.subscriptionId());
            assertThat(row.getCorrelationId()).isEqualTo(event.correlationId());
            assertThat(row.isProcessed()).isFalse();
            assertThat(row.getProcessedOn()).isNull();
            assertThat(row.getAttemptCount()).isZero();
            assertThat(row.getLastError()).isNull();
            assertThat(row.getCreated()).isEqualTo(row.getAvailableAt());
            assertThat(row.getLockedUntil()).isNull();
            assertThat(row.getLockedBy()).isNull();
            assertThat(row.getTraceId()).isNull();
            assertThat(row.getSpanId()).isNull();
        });
    }

    @Test
    void persistsNoOutboxMessageForInternalDeliveryConfirmedEvent() {
        CreatedSubscription created = createSubscriptionThroughPipeline();
        int outboxCountBeforeConfirmation = outboxJpaRepository.findAll().size();
        DeliveryDayId deliveryDayId = transactionTemplate.execute(status -> subscriptionRepository.findById(created.subscriptionId())
                .orElseThrow().getDeliveryCalendar().getDeliveryDays().get(0).getId());

        pipeline.send(new ConfirmDeliveryCommand(created.subscriptionId(), deliveryDayId));

        DeliveryDayStatus updatedStatus = transactionTemplate.execute(status -> subscriptionRepository.findById(created.subscriptionId())
                .orElseThrow()
                .getDeliveryCalendar().getDeliveryDays().stream()
                .filter(day -> day.getId().equals(deliveryDayId))
                .findFirst().orElseThrow().getStatus());
        assertThat(updatedStatus).isEqualTo(DeliveryDayStatus.DELIVERED);
        assertThat(outboxJpaRepository.findAll()).hasSize(outboxCountBeforeConfirmation);
    }

    @Test
    void rollsBackSubscriptionAndRetainsAggregateEventsWhenACommittedDuplicateOutboxIdIsFlushed() {
        SubscriptionId subscriptionId = SubscriptionId.of("SUS-" + UUID.randomUUID());
        Subscription subscription = subscriptionWithKnownCreatedEvent(subscriptionId, "duplicate-correlation");
        SubscriptionCreated event = (SubscriptionCreated) subscription.getDomainEvents().get(0);
        OutboxMessage original = new OutboxMessage(event.eventId(), "subscription.created", "{\"preexisting\":true}",
                Instant.parse("2026-10-29T12:00:00Z"), false, null, 0, null,
                Instant.parse("2026-10-29T12:00:00Z"), null, null, "original-correlation", null, null);
        transactionTemplate.executeWithoutResult(status -> outboxMessageRepository.save(original));

        assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> {
            subscriptionRepository.save(subscription);
            dispatcher.dispatch();
        })).isInstanceOf(RuntimeException.class);

        assertThat(subscriptionJpaRepository.findById(subscriptionId.value())).isEmpty();
        assertThat(outboxMessageRepository.findById(event.eventId())).contains(original);
        assertThat(outboxJpaRepository.findAll().stream().filter(row -> row.getId().equals(event.eventId()))).hasSize(1);
        assertThat(subscription.getDomainEvents()).containsExactly(event);
    }

    @Test
    void leavesNoSubscriptionOrOutboxMessageWhenTheCommandFailsBeforeDispatch() {
        long subscriptionsBefore = subscriptionJpaRepository.count();
        long outboxBefore = outboxJpaRepository.count();
        LocalDate start = LocalDate.now().plusDays(3);
        CreateSubscriptionCommand invalidCommand = new CreateSubscriptionCommand("PAC-" + UUID.randomUUID(), "PLAN-" + UUID.randomUUID(),
                start, start.plusDays(14), ServiceType.LUNCH.name(), new BigDecimal("249.90"), "Accepted terms",
                "DIR-" + UUID.randomUUID(), LocalTime.of(12, 0), LocalTime.of(14, 0), null);

        assertThatThrownBy(() -> pipeline.send(invalidCommand)).isInstanceOf(DomainException.class);

        assertThat(subscriptionJpaRepository.count()).isEqualTo(subscriptionsBefore);
        assertThat(outboxJpaRepository.count()).isEqualTo(outboxBefore);
    }

    private CreatedSubscription createSubscriptionThroughPipeline() {
        String patientId = "PAC-" + UUID.randomUUID();
        patientFixture.ingestEligiblePatient(patientId);
        String addressId = addressFixture.ingestAddress(patientId);
        LocalDate start = LocalDate.now().plusDays(3);
        String dietPlanId = dietPlanFixture.ingestActiveBasePlan(start, 15);
        SubscriptionId subscriptionId = pipeline.send(new CreateSubscriptionCommand(patientId, dietPlanId, start, start.plusDays(14),
                ServiceType.LUNCH.name(), new BigDecimal("249.90"), "Accepted terms", addressId,
                LocalTime.of(12, 0), LocalTime.of(14, 0), null));
        return new CreatedSubscription(subscriptionId);
    }

    private List<OutboxMessageJpaEntity> subscriptionCreatedRowsFor(SubscriptionId subscriptionId) {
        return outboxJpaRepository.findAll().stream()
                .filter(row -> "subscription.created".equals(row.getType()))
                .filter(row -> codec.decode(row.getType(), row.getContent()) instanceof SubscriptionCreated event
                        && event.subscriptionId().equals(subscriptionId))
                .toList();
    }

    private Subscription subscriptionWithKnownCreatedEvent(SubscriptionId subscriptionId, String correlationId) {
        LocalDate start = LocalDate.now().plusDays(3);
        PatientId patientId = PatientId.of("PAC-" + UUID.randomUUID());
        DietPlanId dietPlanId = DietPlanId.of("PLAN-" + UUID.randomUUID());
        ServiceContract contract = new ServiceContract(dietPlanId, PlanType.BASE,
                new ValidityPeriod(start, start.plusDays(14)), ServiceType.LUNCH, new BigDecimal("249.90"),
                "Accepted terms", Instant.now());
        DeliveryAddress address = new DeliveryAddress(AddressId.of("DIR-" + UUID.randomUUID()), "Home", "Main Street", "10", "Cochabamba",
                "Near park", -17.3935, -66.1570, null);
        return Subscription.create(subscriptionId, patientId, contract.dietPlanId(), contract,
                new DeliveryPreferences(address, new TimeWindow(LocalTime.of(12, 0), LocalTime.of(14, 0)), null),
                subscriptionRepository.getNextContractSequenceOfYear(), List.of(), correlationId);
    }

    private record CreatedSubscription(SubscriptionId subscriptionId) {
    }
}
