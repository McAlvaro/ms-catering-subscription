package com.mcalvaro.mscatering.application.subscription.PauseSubscription;

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
class PauseSubscriptionCommandHandlerTest {

    @Mock
    private ISubscriptionRepository subscriptionRepository;

    @InjectMocks
    private PauseSubscriptionCommandHandler handler;

    @Test
    @DisplayName("Should pause the subscription and save the exact aggregate when the command is valid")
    void shouldPauseSubscriptionAndSaveItWhenCommandIsValid() {
        // Arrange
        Subscription subscription = buildSubscription(LocalDate.now().plusDays(10));
        PauseSubscriptionCommand command = buildValidCommand(subscription.getId());
        when(subscriptionRepository.findById(command.subscriptionId())).thenReturn(Optional.of(subscription));

        // Act
        handler.handle(command);

        // Assert
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.PAUSED);
        assertThat(subscription.getPauseRequests()).singleElement().satisfies(request -> {
            assertThat(request.getRange().startDate()).isEqualTo(command.startDate());
            assertThat(request.getRange().endDate()).isEqualTo(command.endDate());
            assertThat(request.getReason()).isEqualTo(command.reason());
        });
        verify(subscriptionRepository, times(1)).findById(command.subscriptionId());
        verify(subscriptionRepository, times(1)).save(subscription);
    }

    @Test
    @DisplayName("Should throw an error and not save when the subscription does not exist")
    void shouldThrowErrorAndNotSaveWhenSubscriptionIsNotFound() {
        // Arrange
        PauseSubscriptionCommand command = buildValidCommand(UUID.randomUUID());
        when(subscriptionRepository.findById(command.subscriptionId())).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> handler.handle(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Subscription not found: " + command.subscriptionId());

        verify(subscriptionRepository, times(1)).findById(command.subscriptionId());
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should propagate DomainException SUB-003 and not save when the subscription is already paused")
    void shouldPropagateDomainExceptionAndNotSaveWhenSubscriptionIsAlreadyPaused() {
        // Arrange
        Subscription subscription = buildSubscription(LocalDate.now().plusDays(10));
        subscription.pause(new com.mcalvaro.mscatering.domain.subscription.vo.PauseRange(
                LocalDate.now().plusDays(3), LocalDate.now().plusDays(5)), "Travel");
        PauseSubscriptionCommand command = buildValidCommand(subscription.getId());
        when(subscriptionRepository.findById(command.subscriptionId())).thenReturn(Optional.of(subscription));

        // Act & Assert
        assertThatThrownBy(() -> handler.handle(command))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("code", "SUB-003");

        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.PAUSED);
        verify(subscriptionRepository, times(1)).findById(command.subscriptionId());
        verify(subscriptionRepository, never()).save(any());
    }

    private PauseSubscriptionCommand buildValidCommand(UUID subscriptionId) {
        return new PauseSubscriptionCommand(subscriptionId, LocalDate.now().plusDays(3), LocalDate.now().plusDays(5), "Travel");
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
