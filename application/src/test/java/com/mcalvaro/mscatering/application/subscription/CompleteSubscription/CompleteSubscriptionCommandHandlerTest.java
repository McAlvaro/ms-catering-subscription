package com.mcalvaro.mscatering.application.subscription.CompleteSubscription;

import com.mcalvaro.mscatering.domain.core.DomainException;
import com.mcalvaro.mscatering.domain.subscription.ISubscriptionRepository;
import com.mcalvaro.mscatering.domain.subscription.Subscription;
import com.mcalvaro.mscatering.domain.subscription.enums.ServiceType;
import com.mcalvaro.mscatering.domain.subscription.enums.SubscriptionStatus;
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
class CompleteSubscriptionCommandHandlerTest {

    @Mock
    private ISubscriptionRepository subscriptionRepository;

    @InjectMocks
    private CompleteSubscriptionCommandHandler handler;

    @Test
    @DisplayName("Should complete the subscription and save the exact aggregate when its contract has ended")
    void shouldCompleteSubscriptionAndSaveItWhenContractHasEnded() {
        // Arrange
        Subscription subscription = buildSubscription(LocalDate.now().minusDays(20));
        CompleteSubscriptionCommand command = new CompleteSubscriptionCommand(subscription.getId());
        when(subscriptionRepository.findById(command.subscriptionId())).thenReturn(Optional.of(subscription));

        // Act
        handler.handle(command);

        // Assert
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.COMPLETED);
        verify(subscriptionRepository, times(1)).findById(command.subscriptionId());
        verify(subscriptionRepository, times(1)).save(subscription);
    }

    @Test
    @DisplayName("Should throw an error and not save when the subscription does not exist")
    void shouldThrowErrorAndNotSaveWhenSubscriptionIsNotFound() {
        // Arrange
        CompleteSubscriptionCommand command = new CompleteSubscriptionCommand(UUID.randomUUID());
        when(subscriptionRepository.findById(command.subscriptionId())).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> handler.handle(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Subscription not found: " + command.subscriptionId());

        verify(subscriptionRepository, times(1)).findById(command.subscriptionId());
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should propagate DomainException SUB-008 and not save when the contract period has not ended")
    void shouldPropagateDomainExceptionAndNotSaveWhenContractPeriodHasNotEnded() {
        // Arrange
        Subscription subscription = buildSubscription(LocalDate.now().plusDays(10));
        CompleteSubscriptionCommand command = new CompleteSubscriptionCommand(subscription.getId());
        when(subscriptionRepository.findById(command.subscriptionId())).thenReturn(Optional.of(subscription));

        // Act & Assert
        assertThatThrownBy(() -> handler.handle(command))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("code", "SUB-008");

        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        verify(subscriptionRepository, times(1)).findById(command.subscriptionId());
        verify(subscriptionRepository, never()).save(any());
    }

    private Subscription buildSubscription(LocalDate startDate) {
        ServiceContract contract = new ServiceContract(UUID.randomUUID(), new ValidityPeriod(startDate, startDate.plusDays(14)),
                ServiceType.FULL, new BigDecimal("250.00"), "Terms accepted", Instant.parse("2025-01-01T10:00:00Z"));
        DeliveryPreferences preferences = new DeliveryPreferences(
                new DeliveryAddress("Main Street", "10", "Madrid", "Door A", 40.4168, -3.7038, "600000000"),
                new TimeWindow(LocalTime.of(9, 0), LocalTime.of(11, 0)), "Leave at reception");
        return Subscription.create(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), contract, preferences, 1);
    }
}
