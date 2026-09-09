package com.mcalvaro.mscatering.application.subscription.RegisterFailedDelivery;

import com.mcalvaro.mscatering.domain.core.DomainException;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterFailedDeliveryCommandHandlerTest {

    @Mock
    private ISubscriptionRepository subscriptionRepository;

    @InjectMocks
    private RegisterFailedDeliveryCommandHandler handler;

    @Test
    @DisplayName("Should mark the delivery as failed with its reason and save the subscription when the day exists")
    void shouldMarkDeliveryAsFailedAndSaveSubscriptionWhenDayExists() {
        // Arrange
        Subscription subscription = buildSubscription();
        DeliveryDay deliveryDay = subscription.getDeliveryCalendar().getDeliveryDays().get(0);
        RegisterFailedDeliveryCommand command = new RegisterFailedDeliveryCommand(subscription.getId(), deliveryDay.getId(), "Recipient unavailable");
        when(subscriptionRepository.findById(command.subscriptionId())).thenReturn(Optional.of(subscription));

        // Act
        handler.handle(command);

        // Assert
        assertThat(deliveryDay.getStatus()).isEqualTo(DeliveryDayStatus.FAILED);
        assertThat(deliveryDay.getFailureReason()).isEqualTo(command.reason());
        verify(subscriptionRepository, times(1)).findById(command.subscriptionId());
        verify(subscriptionRepository, times(1)).save(subscription);
    }

    @Test
    @DisplayName("Should throw an exception and not save when the subscription is absent")
    void shouldThrowExceptionAndNotSaveWhenSubscriptionIsAbsent() {
        // Arrange
        RegisterFailedDeliveryCommand command = new RegisterFailedDeliveryCommand(UUID.randomUUID(), UUID.randomUUID(), "Recipient unavailable");
        when(subscriptionRepository.findById(command.subscriptionId())).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> handler.handle(command)).isInstanceOf(IllegalArgumentException.class);
        verify(subscriptionRepository, times(1)).findById(command.subscriptionId());
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should propagate the aggregate exception and not save when the delivery day is unknown")
    void shouldPropagateAggregateExceptionAndNotSaveWhenDeliveryDayIsUnknown() {
        // Arrange
        Subscription subscription = buildSubscription();
        RegisterFailedDeliveryCommand command = new RegisterFailedDeliveryCommand(subscription.getId(), UUID.randomUUID(), "Recipient unavailable");
        when(subscriptionRepository.findById(command.subscriptionId())).thenReturn(Optional.of(subscription));

        // Act & Assert
        assertThatThrownBy(() -> handler.handle(command)).isInstanceOf(DomainException.class).hasFieldOrPropertyWithValue("code", "SUB-009");
        verify(subscriptionRepository, never()).save(any());
    }

    private Subscription buildSubscription() {
        LocalDate startDate = LocalDate.of(2099, 1, 1);
        DeliveryAddress address = new DeliveryAddress("Main Street", "10", "Madrid", "Door A", 40.4168, -3.7038, "600000000");
        return Subscription.create(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), new ServiceContract(UUID.randomUUID(), new ValidityPeriod(startDate, startDate.plusDays(14)), ServiceType.FULL, new BigDecimal("250.00"), "Terms accepted", Instant.parse("2025-01-01T10:00:00Z")), new DeliveryPreferences(address, new TimeWindow(LocalTime.of(9, 0), LocalTime.of(11, 0)), "Leave at reception"), 1);
    }
}
