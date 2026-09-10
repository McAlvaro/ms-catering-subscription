package com.mcalvaro.mscatering.infrastructure.persistence.subscription.mapper;

import com.mcalvaro.mscatering.domain.subscription.Subscription;
import com.mcalvaro.mscatering.domain.subscription.entity.BiweeklyEvaluation;
import com.mcalvaro.mscatering.domain.subscription.entity.DeliveryDay;
import com.mcalvaro.mscatering.domain.subscription.enums.DeliveryDayStatus;
import com.mcalvaro.mscatering.domain.subscription.enums.EvaluationStatus;
import com.mcalvaro.mscatering.domain.subscription.enums.ServiceType;
import com.mcalvaro.mscatering.domain.subscription.enums.SubscriptionStatus;
import com.mcalvaro.mscatering.domain.subscription.vo.DeliveryAddress;
import com.mcalvaro.mscatering.domain.subscription.vo.DeliveryPreferences;
import com.mcalvaro.mscatering.domain.subscription.vo.PauseRange;
import com.mcalvaro.mscatering.domain.subscription.vo.ServiceContract;
import com.mcalvaro.mscatering.domain.subscription.vo.TimeWindow;
import com.mcalvaro.mscatering.domain.subscription.vo.ValidityPeriod;
import com.mcalvaro.mscatering.infrastructure.persistence.subscription.entity.BiweeklyEvaluationJpaEntity;
import com.mcalvaro.mscatering.infrastructure.persistence.subscription.entity.DeliveryCalendarJpaEntity;
import com.mcalvaro.mscatering.infrastructure.persistence.subscription.entity.DeliveryDayJpaEntity;
import com.mcalvaro.mscatering.infrastructure.persistence.subscription.entity.PauseRequestJpaEntity;
import com.mcalvaro.mscatering.infrastructure.persistence.subscription.entity.SubscriptionJpaEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SubscriptionMapperTest {

    private final SubscriptionMapper mapper = new SubscriptionMapper();

    @Test
    @DisplayName("Should map subscription scalars, delivery calendar, pauses and evaluations to JPA")
    void shouldMapSubscriptionToJpaWhenDomainSubscriptionContainsNestedEntities() {
        // Arrange
        Subscription subscription = buildSubscriptionWithNestedState();

        // Act
        SubscriptionJpaEntity result = mapper.toJpaEntity(subscription);

        // Assert
        assertThat(result.getId()).isEqualTo(subscription.getId());
        assertThat(result.getPatientId()).isEqualTo(subscription.getPatientId());
        assertThat(result.getDietPlanId()).isEqualTo(subscription.getDietPlanId());
        assertThat(result.getContractCode()).isEqualTo(subscription.getContractCode().value());
        assertThat(result.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE.name());
        assertThat(result.getContractStartDate()).isEqualTo(subscription.getContract().period().startDate());
        assertThat(result.getContractEndDate()).isEqualTo(subscription.getContract().period().endDate());
        assertThat(result.getContractDurationDays()).isEqualTo(subscription.getContract().period().durationDays());
        assertThat(result.getServiceType()).isEqualTo(subscription.getContract().serviceType().name());
        assertThat(result.getTotalPrice()).isEqualByComparingTo(subscription.getContract().totalPrice());
        assertThat(result.getAcceptedConditions()).isEqualTo(subscription.getContract().acceptedConditions());
        assertThat(result.getSignedAt()).isEqualTo(subscription.getContract().signedAt());
        assertThat(result.getPrefStreet()).isEqualTo("Main Street");
        assertThat(result.getPrefNumber()).isEqualTo("42");
        assertThat(result.getPrefCity()).isEqualTo("Madrid");
        assertThat(result.getPrefReference()).isEqualTo("Blue door");
        assertThat(result.getPrefLatitude()).isEqualTo(40.4168);
        assertThat(result.getPrefLongitude()).isEqualTo(-3.7038);
        assertThat(result.getPrefPhone()).isEqualTo("+34123456789");
        assertThat(result.getPrefTimeStart()).isEqualTo("09:00");
        assertThat(result.getPrefTimeEnd()).isEqualTo("11:00");
        assertThat(result.getPrefSpecialInstructions()).isEqualTo("Ring the bell");
        assertThat(result.getDeliveryCalendar().getId()).isEqualTo(subscription.getDeliveryCalendar().getId());
        assertThat(result.getDeliveryCalendar().getPeriodStart())
                .isEqualTo(subscription.getContract().period().startDate());
        assertThat(result.getDeliveryCalendar().getPeriodEnd())
                .isEqualTo(subscription.getContract().period().endDate());
        assertThat(result.getDeliveryCalendar().getSubscription()).isSameAs(result);
        assertThat(result.getDeliveryCalendar().getDeliveryDays()).hasSize(15);
        assertThat(result.getDeliveryCalendar().getDeliveryDays().get(0)).satisfies(day -> {
            assertThat(day.getId()).isEqualTo(subscription.getDeliveryCalendar().getDeliveryDays().get(0).getId());
            assertThat(day.getDate()).isEqualTo(LocalDate.of(2027, 7, 1));
            assertThat(day.getStatus()).isEqualTo(DeliveryDayStatus.DELIVERED.name());
            assertThat(day.getAddressStreet()).isEqualTo("Main Street");
            assertThat(day.getAddressNumber()).isEqualTo("42");
            assertThat(day.getAddressCity()).isEqualTo("Madrid");
            assertThat(day.getAddressReference()).isEqualTo("Blue door");
            assertThat(day.getAddressLatitude()).isEqualTo(40.4168);
            assertThat(day.getAddressLongitude()).isEqualTo(-3.7038);
            assertThat(day.getAddressPhone()).isEqualTo("+34123456789");
            assertThat(day.getTimeStart()).isEqualTo("09:00");
            assertThat(day.getTimeEnd()).isEqualTo("11:00");
            assertThat(day.getInstructions()).isEqualTo("Ring the bell");
            assertThat(day.getDeliveredAt()).isNotNull();
            assertThat(day.getConsolidatedAt()).isNull();
            assertThat(day.getFailureReason()).isNull();
        });
        assertThat(result.getDeliveryCalendar().getDeliveryDays().get(1).getStatus())
                .isEqualTo(DeliveryDayStatus.FAILED.name());
        assertThat(result.getDeliveryCalendar().getDeliveryDays().get(1).getFailureReason())
                .isEqualTo("Patient unavailable");
        assertThat(result.getDeliveryCalendar().getDeliveryDays().get(2).getStatus())
                .isEqualTo(DeliveryDayStatus.CONSOLIDATED.name());
        assertThat(result.getDeliveryCalendar().getDeliveryDays().get(2).getConsolidatedAt()).isNotNull();
        assertThat(result.getPauseRequests()).singleElement().satisfies(pause -> {
            assertThat(pause.getId()).isEqualTo(subscription.getPauseRequests().get(0).getId());
            assertThat(pause.getRangeStart()).isEqualTo(LocalDate.of(2027, 7, 4));
            assertThat(pause.getRangeEnd()).isEqualTo(LocalDate.of(2027, 7, 5));
            assertThat(pause.getReason()).isEqualTo("Medical appointment");
            assertThat(pause.getActualEndDate()).isEqualTo(LocalDate.of(2027, 7, 4));
            assertThat(pause.getCreatedAt()).isNotNull();
            assertThat(pause.isActive()).isFalse();
            assertThat(pause.getSubscription()).isSameAs(result);
        });
        assertThat(result.getEvaluations()).extracting(BiweeklyEvaluationJpaEntity::getId)
                .containsExactlyElementsOf(
                        subscription.getEvaluations().stream().map(BiweeklyEvaluation::getId).toList());
        assertThat(result.getEvaluations()).extracting(BiweeklyEvaluationJpaEntity::getStatus)
                .containsExactly(EvaluationStatus.COMPLETED.name(), EvaluationStatus.CANCELLED.name());
        assertThat(result.getEvaluations().get(0).getCompletedDate()).isEqualTo(LocalDate.of(2027, 7, 15));
        assertThat(result.getEvaluations().get(1).getCompletedDate()).isNull();
    }

    @Test
    @DisplayName("Should map JPA subscription fields, nullable coordinates and nested state to domain")
    void shouldMapSubscriptionToDomainWhenJpaSubscriptionContainsNestedEntities() {
        // Arrange
        SubscriptionJpaEntity subscription = buildSubscriptionJpaEntity();

        // Act
        Subscription result = mapper.toDomain(subscription);

        // Assert
        assertThat(result.getId()).isEqualTo(subscription.getId());
        assertThat(result.getPatientId()).isEqualTo(subscription.getPatientId());
        assertThat(result.getDietPlanId()).isEqualTo(subscription.getDietPlanId());
        assertThat(result.getContractCode().value()).isEqualTo(subscription.getContractCode());
        assertThat(result.getStatus()).isEqualTo(SubscriptionStatus.PAUSED);
        assertThat(result.getContract().period().startDate()).isEqualTo(subscription.getContractStartDate());
        assertThat(result.getContract().period().endDate()).isEqualTo(subscription.getContractEndDate());
        assertThat(result.getContract().serviceType().name()).isEqualTo(subscription.getServiceType());
        assertThat(result.getContract().totalPrice()).isEqualByComparingTo(subscription.getTotalPrice());
        assertThat(result.getContract().acceptedConditions()).isEqualTo(subscription.getAcceptedConditions());
        assertThat(result.getContract().signedAt()).isEqualTo(subscription.getSignedAt());
        assertThat(result.getPreferences().primaryAddress().street()).isEqualTo("Oak Avenue");
        assertThat(result.getPreferences().primaryAddress().number()).isNull();
        assertThat(result.getPreferences().primaryAddress().city()).isEqualTo("Seville");
        assertThat(result.getPreferences().primaryAddress().reference()).isNull();
        assertThat(result.getPreferences().primaryAddress().latitude()).isZero();
        assertThat(result.getPreferences().primaryAddress().longitude()).isZero();
        assertThat(result.getPreferences().primaryAddress().phone()).isEqualTo("+34987654321");
        assertThat(result.getPreferences().timeWindow().startTime()).isEqualTo(LocalTime.of(12, 0));
        assertThat(result.getPreferences().timeWindow().endTime()).isEqualTo(LocalTime.of(14, 0));
        assertThat(result.getPreferences().specialInstructions()).isNull();
        assertThat(result.getDeliveryCalendar().getId()).isEqualTo(subscription.getDeliveryCalendar().getId());
        assertThat(result.getDeliveryCalendar().getSubscriptionId()).isEqualTo(subscription.getId());
        assertThat(result.getDeliveryCalendar().getDeliveryDays()).singleElement().satisfies(day -> {
            assertThat(day.getId()).isEqualTo(subscription.getDeliveryCalendar().getDeliveryDays().get(0).getId());
            assertThat(day.getDate()).isEqualTo(LocalDate.of(2027, 8, 2));
            assertThat(day.getStatus()).isEqualTo(DeliveryDayStatus.FAILED);
            assertThat(day.getAddress().latitude()).isZero();
            assertThat(day.getAddress().longitude()).isZero();
            assertThat(day.getFailureReason()).isEqualTo("Address inaccessible");
            assertThat(day.getDeliveredAt()).isNull();
            assertThat(day.getConsolidatedAt()).isNull();
        });
        assertThat(result.getPauseRequests()).singleElement().satisfies(pause -> {
            assertThat(pause.getId()).isEqualTo(subscription.getPauseRequests().get(0).getId());
            assertThat(pause.getRange().startDate()).isEqualTo(LocalDate.of(2027, 8, 4));
            assertThat(pause.getRange().endDate()).isEqualTo(LocalDate.of(2027, 8, 6));
            assertThat(pause.getReason()).isEqualTo("Hospitalization");
            assertThat(pause.getActualEndDate()).isNull();
            assertThat(pause.getCreatedAt()).isEqualTo(Instant.parse("2027-07-29T08:00:00Z"));
            assertThat(pause.isActive()).isTrue();
        });
        assertThat(result.getEvaluations()).singleElement().satisfies(evaluation -> {
            assertThat(evaluation.getId()).isEqualTo(subscription.getEvaluations().get(0).getId());
            assertThat(evaluation.getPatientId()).isEqualTo(subscription.getPatientId());
            assertThat(evaluation.getEvaluationNumber()).isEqualTo(3);
            assertThat(evaluation.getScheduledDate()).isEqualTo(LocalDate.of(2027, 8, 15));
            assertThat(evaluation.getStatus()).isEqualTo(EvaluationStatus.COMPLETED);
            assertThat(evaluation.getCompletedDate()).isEqualTo(LocalDate.of(2027, 8, 16));
        });
    }

    @Test
    @DisplayName("Should return null when the subscription source is null")
    void shouldReturnNullWhenSubscriptionSourceIsNull() {
        // Arrange
        Subscription domainSubscription = null;
        SubscriptionJpaEntity jpaSubscription = null;

        // Act
        SubscriptionJpaEntity jpaResult = mapper.toJpaEntity(domainSubscription);
        Subscription domainResult = mapper.toDomain(jpaSubscription);

        // Assert
        assertThat(jpaResult).isNull();
        assertThat(domainResult).isNull();
    }

    private Subscription buildSubscriptionWithNestedState() {
        UUID subscriptionId = UUID.fromString("ef19967c-98ad-4629-a8c9-69f3b81e3312");
        Subscription subscription = Subscription.create(subscriptionId,
                UUID.fromString("fc3a747c-4b2f-4701-83fb-2f6f941f0cf5"),
                UUID.fromString("cea36c1d-e158-4827-84e2-1dd095550fcb"), buildContract(), buildPreferences(), 7);
        List<DeliveryDay> days = subscription.getDeliveryCalendar().getDeliveryDays();
        days.get(0).markAsDelivered();
        days.get(1).markAsFailed("Patient unavailable");
        days.get(2).markAsConsolidated();
        subscription.pause(new PauseRange(LocalDate.of(2027, 7, 4), LocalDate.of(2027, 7, 5)), "Medical appointment");
        subscription.reactivate(LocalDate.of(2027, 7, 4));
        BiweeklyEvaluation completed = new BiweeklyEvaluation(UUID.fromString("f2b985c0-67b8-4d1d-a54b-47904d8c3b0a"),
                subscription.getPatientId(), 1, LocalDate.of(2027, 7, 14));
        completed.markCompleted(LocalDate.of(2027, 7, 15));
        BiweeklyEvaluation cancelled = new BiweeklyEvaluation(UUID.fromString("a2f2fe58-b6c2-47a2-9c24-ecbc0961c989"),
                subscription.getPatientId(), 2, LocalDate.of(2027, 7, 28));
        cancelled.cancel();
        subscription.scheduleEvaluations(List.of(completed, cancelled));
        return subscription;
    }

    private SubscriptionJpaEntity buildSubscriptionJpaEntity() {
        Subscription source = buildSubscriptionWithNestedState();
        SubscriptionJpaEntity subscription = new SubscriptionJpaEntity();
        subscription.setId(UUID.fromString("d7f5c16c-ca8d-4828-97b4-bfc9316ebd3a"));
        subscription.setPatientId(UUID.fromString("2b6d5d06-94e8-4ab8-8ea1-1648016be289"));
        subscription.setDietPlanId(UUID.fromString("e9d0f851-f385-45e8-b6fe-931678dd7a25"));
        subscription.setContractCode(source.getContractCode().value());
        subscription.setStatus(SubscriptionStatus.PAUSED.name());
        subscription.setContractStartDate(LocalDate.of(2027, 8, 1));
        subscription.setContractEndDate(LocalDate.of(2027, 8, 15));
        subscription.setContractDurationDays(15);
        subscription.setServiceType(ServiceType.values()[0].name());
        subscription.setTotalPrice(new BigDecimal("175.50"));
        subscription.setAcceptedConditions("Conditions were declined");
        subscription.setSignedAt(Instant.parse("2027-07-20T09:30:00Z"));
        subscription.setPrefStreet("Oak Avenue");
        subscription.setPrefNumber(null);
        subscription.setPrefCity("Seville");
        subscription.setPrefReference(null);
        subscription.setPrefLatitude(null);
        subscription.setPrefLongitude(null);
        subscription.setPrefPhone("+34987654321");
        subscription.setPrefTimeStart("12:00");
        subscription.setPrefTimeEnd("14:00");
        subscription.setPrefSpecialInstructions(null);
        DeliveryCalendarJpaEntity calendar = new DeliveryCalendarJpaEntity();
        calendar.setId(UUID.fromString("b290c4c9-1a59-4e35-a4be-f59e56f9e70c"));
        calendar.setPeriodStart(LocalDate.of(2027, 8, 1));
        calendar.setPeriodEnd(LocalDate.of(2027, 8, 15));
        DeliveryDayJpaEntity day = new DeliveryDayJpaEntity();
        day.setId(UUID.fromString("42e1e838-1cf6-4c86-9f7a-02a2dc7d9129"));
        day.setDate(LocalDate.of(2027, 8, 2));
        day.setStatus(DeliveryDayStatus.FAILED.name());
        day.setAddressStreet("Oak Avenue");
        day.setAddressNumber(null);
        day.setAddressCity("Seville");
        day.setAddressReference(null);
        day.setAddressLatitude(null);
        day.setAddressLongitude(null);
        day.setAddressPhone("+34987654321");
        day.setTimeStart("12:00");
        day.setTimeEnd("14:00");
        day.setInstructions(null);
        day.setConsolidatedAt(null);
        day.setDeliveredAt(null);
        day.setFailureReason("Address inaccessible");
        calendar.setDeliveryDays(List.of(day));
        subscription.setDeliveryCalendar(calendar);
        PauseRequestJpaEntity pause = new PauseRequestJpaEntity();
        pause.setId(UUID.fromString("58fe944f-8d34-437b-9083-7f0b4b7c6e8f"));
        pause.setRangeStart(LocalDate.of(2027, 8, 4));
        pause.setRangeEnd(LocalDate.of(2027, 8, 6));
        pause.setReason("Hospitalization");
        pause.setActualEndDate(null);
        pause.setCreatedAt(Instant.parse("2027-07-29T08:00:00Z"));
        pause.setActive(true);
        subscription.setPauseRequests(List.of(pause));
        BiweeklyEvaluationJpaEntity evaluation = new BiweeklyEvaluationJpaEntity();
        evaluation.setId(UUID.fromString("5da85e98-1c52-4c09-b86d-21f30d7b3640"));
        evaluation.setPatientId(subscription.getPatientId());
        evaluation.setEvaluationNumber(3);
        evaluation.setScheduledDate(LocalDate.of(2027, 8, 15));
        evaluation.setStatus(EvaluationStatus.COMPLETED.name());
        evaluation.setCompletedDate(LocalDate.of(2027, 8, 16));
        subscription.setEvaluations(List.of(evaluation));
        return subscription;
    }

    private ServiceContract buildContract() {
        return new ServiceContract(UUID.fromString("cea36c1d-e158-4827-84e2-1dd095550fcb"),
                new ValidityPeriod(LocalDate.of(2027, 7, 1), LocalDate.of(2027, 7, 15)), ServiceType.values()[0],
                new BigDecimal("199.99"), "Conditions were accepted", Instant.parse("2027-06-20T10:30:00Z"));
    }

    private DeliveryPreferences buildPreferences() {
        return new DeliveryPreferences(new DeliveryAddress("Main Street", "42", "Madrid", "Blue door", 40.4168,
                -3.7038, "+34123456789"), new TimeWindow(LocalTime.of(9, 0), LocalTime.of(11, 0)), "Ring the bell");
    }
}
