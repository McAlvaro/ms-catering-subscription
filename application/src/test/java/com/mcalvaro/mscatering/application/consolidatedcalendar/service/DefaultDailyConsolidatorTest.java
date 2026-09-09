package com.mcalvaro.mscatering.application.consolidatedcalendar.service;

import com.mcalvaro.mscatering.domain.consolidatedcalendar.ConsolidatedCalendar;
import com.mcalvaro.mscatering.domain.consolidatedcalendar.entity.ConsolidatedLine;
import com.mcalvaro.mscatering.domain.consolidatedcalendar.enums.ConsolidateStatus;
import com.mcalvaro.mscatering.domain.subscription.ISubscriptionRepository;
import com.mcalvaro.mscatering.domain.subscription.Subscription;
import com.mcalvaro.mscatering.domain.subscription.entity.DeliveryDay;
import com.mcalvaro.mscatering.domain.subscription.enums.DeliveryDayStatus;
import com.mcalvaro.mscatering.domain.subscription.enums.ServiceType;
import com.mcalvaro.mscatering.domain.subscription.vo.DeliveryAddress;
import com.mcalvaro.mscatering.domain.subscription.vo.DeliveryPreferences;
import com.mcalvaro.mscatering.domain.subscription.vo.ServiceContract;
import com.mcalvaro.mscatering.domain.subscription.vo.TimeWindow;
import com.mcalvaro.mscatering.domain.subscription.vo.ValidityPeriod;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultDailyConsolidatorTest {

    @Mock
    private ISubscriptionRepository subscriptionRepository;

    @InjectMocks
    private DefaultDailyConsolidator consolidator;

    @Test
    @DisplayName("Should add a line, consolidate the delivery day and save its subscription when the day is scheduled")
    void shouldAddLineConsolidateDeliveryDayAndSaveSubscriptionWhenDayIsScheduled() {
        // Arrange
        LocalDate deliveryDate = LocalDate.of(2026, 9, 7);
        Subscription subscription = buildSubscription(deliveryDate);
        DeliveryDay deliveryDay = subscription.getDeliveryCalendar().getDaysForDate(deliveryDate).get(0);
        when(subscriptionRepository.findActiveSubscriptions()).thenReturn(List.of(subscription));

        // Act
        ConsolidatedCalendar calendar = consolidator.consolidateForDate(deliveryDate);

        // Assert
        assertThat(calendar.getDate()).isEqualTo(deliveryDate);
        assertThat(calendar.getStatus()).isEqualTo(ConsolidateStatus.OPEN);
        assertThat(calendar.getTotalDeliveries()).isEqualTo(1);
        assertThat(calendar.getLines()).hasSize(1);
        assertThat(deliveryDay.getStatus()).isEqualTo(DeliveryDayStatus.CONSOLIDATED);
        assertThat(deliveryDay.getConsolidatedAt()).isNotNull();

        ConsolidatedLine line = calendar.getLines().get(0);
        assertThat(line.getConsolidatedCalendarId()).isEqualTo(calendar.getId());
        assertThat(line.getSubscriptionId()).isEqualTo(subscription.getId());
        assertThat(line.getPatientId()).isEqualTo(subscription.getPatientId());
        assertThat(line.getDietPlanId()).isEqualTo(subscription.getDietPlanId());
        assertThat(line.getServiceType()).isEqualTo(ServiceType.LUNCH);
        assertThat(line.getAddress()).isEqualTo(deliveryDay.getAddress());
        assertThat(line.getTimeWindow()).isEqualTo(deliveryDay.getTimeWindow());
        assertThat(line.getInstructions()).isEqualTo(deliveryDay.getInstructions());

        verify(subscriptionRepository, times(1)).findActiveSubscriptions();
        verify(subscriptionRepository, times(1)).save(subscription);
    }

    @Test
    @DisplayName("Should not add a line, modify the delivery day or save the subscription when the day is not scheduled")
    void shouldNotAddLineModifyDeliveryDayOrSaveSubscriptionWhenDayIsNotScheduled() {
        // Arrange
        LocalDate deliveryDate = LocalDate.of(2026, 9, 7);
        Subscription subscription = buildSubscription(deliveryDate);
        DeliveryDay deliveryDay = subscription.getDeliveryCalendar().getDaysForDate(deliveryDate).get(0);
        deliveryDay.pause();
        when(subscriptionRepository.findActiveSubscriptions()).thenReturn(List.of(subscription));

        // Act
        ConsolidatedCalendar calendar = consolidator.consolidateForDate(deliveryDate);

        // Assert
        assertThat(calendar.getStatus()).isEqualTo(ConsolidateStatus.OPEN);
        assertThat(calendar.getLines()).isEmpty();
        assertThat(calendar.getTotalDeliveries()).isZero();
        assertThat(deliveryDay.getStatus()).isEqualTo(DeliveryDayStatus.PAUSED);
        assertThat(deliveryDay.getConsolidatedAt()).isNull();

        verify(subscriptionRepository, times(1)).findActiveSubscriptions();
        verify(subscriptionRepository, never()).save(subscription);
    }

    @Test
    @DisplayName("Should return an open calendar without lines or saves when there are no active subscriptions")
    void shouldReturnOpenCalendarWithoutLinesOrSavesWhenThereAreNoActiveSubscriptions() {
        // Arrange
        LocalDate deliveryDate = LocalDate.of(2026, 9, 7);
        when(subscriptionRepository.findActiveSubscriptions()).thenReturn(List.of());

        // Act
        ConsolidatedCalendar calendar = consolidator.consolidateForDate(deliveryDate);

        // Assert
        assertThat(calendar.getDate()).isEqualTo(deliveryDate);
        assertThat(calendar.getStatus()).isEqualTo(ConsolidateStatus.OPEN);
        assertThat(calendar.getLines()).isEmpty();
        assertThat(calendar.getTotalDeliveries()).isZero();

        verify(subscriptionRepository, times(1)).findActiveSubscriptions();
        verify(subscriptionRepository, never()).save(any(Subscription.class));
    }

    @Test
    @DisplayName("Should return an open calendar without lines or saves when active subscriptions have no matching delivery days")
    void shouldReturnOpenCalendarWithoutLinesOrSavesWhenActiveSubscriptionsHaveNoMatchingDeliveryDays() {
        // Arrange
        LocalDate deliveryDate = LocalDate.of(2026, 9, 7);
        Subscription subscription = buildSubscription(deliveryDate.plusDays(1));
        when(subscriptionRepository.findActiveSubscriptions()).thenReturn(List.of(subscription));

        // Act
        ConsolidatedCalendar calendar = consolidator.consolidateForDate(deliveryDate);

        // Assert
        assertThat(calendar.getDate()).isEqualTo(deliveryDate);
        assertThat(calendar.getStatus()).isEqualTo(ConsolidateStatus.OPEN);
        assertThat(calendar.getLines()).isEmpty();
        assertThat(calendar.getTotalDeliveries()).isZero();

        verify(subscriptionRepository, times(1)).findActiveSubscriptions();
        verify(subscriptionRepository, never()).save(subscription);
    }

    private Subscription buildSubscription(LocalDate startDate) {
        DeliveryAddress address = new DeliveryAddress(
                "Main Street", "123", "Madrid", "Reception", 40.4168, -3.7038, "+34123456789");
        TimeWindow timeWindow = new TimeWindow(LocalTime.of(12, 0), LocalTime.of(14, 0));
        DeliveryPreferences preferences = new DeliveryPreferences(address, timeWindow, "Ring the doorbell");
        ServiceContract contract = new ServiceContract(
                UUID.randomUUID(),
                new ValidityPeriod(startDate, startDate.plusDays(14)),
                ServiceType.LUNCH,
                new BigDecimal("150.00"),
                "Accepted terms",
                Instant.parse("2026-09-01T10:00:00Z"));

        return Subscription.create(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), contract, preferences, 1);
    }
}
