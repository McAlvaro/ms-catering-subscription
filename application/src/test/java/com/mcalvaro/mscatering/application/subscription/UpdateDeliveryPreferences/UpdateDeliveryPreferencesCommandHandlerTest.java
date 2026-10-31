package com.mcalvaro.mscatering.application.subscription.UpdateDeliveryPreferences;

import com.mcalvaro.mscatering.domain.core.DomainException;
import com.mcalvaro.mscatering.domain.core.id.AddressId;
import com.mcalvaro.mscatering.domain.core.id.DietPlanId;
import com.mcalvaro.mscatering.domain.core.id.PatientId;
import com.mcalvaro.mscatering.domain.core.id.SubscriptionId;
import com.mcalvaro.mscatering.domain.subscription.ISubscriptionRepository;
import com.mcalvaro.mscatering.domain.subscription.Subscription;
import com.mcalvaro.mscatering.domain.subscription.enums.ServiceType;
import com.mcalvaro.mscatering.domain.subscription.vo.DeliveryAddress;
import com.mcalvaro.mscatering.domain.subscription.vo.DeliveryPreferences;
import com.mcalvaro.mscatering.domain.subscription.vo.ServiceContract;
import com.mcalvaro.mscatering.domain.subscription.vo.TimeWindow;
import com.mcalvaro.mscatering.domain.subscription.vo.ValidityPeriod;
import com.mcalvaro.mscatering.application.patient.address.DeliveryAddressResolver;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateDeliveryPreferencesCommandHandlerTest {

    private static final AddressId DELIVERY_ADDRESS_ID = AddressId.of("DIR-550e8400-e29b-41d4-a716-446655440011");
    private static final AddressId FOREIGN_ADDRESS_ID = AddressId.of("DIR-550e8400-e29b-41d4-a716-446655440012");

    @Mock
    private ISubscriptionRepository subscriptionRepository;

    @Mock
    private DeliveryAddressResolver deliveryAddressResolver;

    @InjectMocks
    private UpdateDeliveryPreferencesCommandHandler handler;

    @Test
    @DisplayName("Should update delivery preferences and save the exact aggregate when the command is valid")
    void shouldUpdateDeliveryPreferencesAndSaveItWhenCommandIsValid() {
        // Arrange
        Subscription subscription = buildSubscription();
        UpdateDeliveryPreferencesCommand command = buildValidCommand(subscription.getId());
        when(subscriptionRepository.findById(command.subscriptionId())).thenReturn(Optional.of(subscription));
        when(deliveryAddressResolver.resolve(subscription.getPatientId(), command.deliveryAddressId()))
                .thenReturn(new DeliveryAddress(command.deliveryAddressId(), "Work", "New Street", null, "Barcelona", "Floor 2", 41.3874, 2.1686, null));

        // Act
        handler.handle(command);

        // Assert
        assertThat(subscription.getPreferences().primaryAddress().street()).isEqualTo("New Street");
        assertThat(subscription.getPreferences().primaryAddress().number()).isNull();
        assertThat(subscription.getPreferences().primaryAddress().city()).isEqualTo("Barcelona");
        assertThat(subscription.getPreferences().timeWindow().startTime()).isEqualTo(command.startTime());
        assertThat(subscription.getPreferences().timeWindow().endTime()).isEqualTo(command.endTime());
        assertThat(subscription.getPreferences().specialInstructions()).isEqualTo(command.specialInstructions());
        verify(subscriptionRepository, times(1)).findById(command.subscriptionId());
        verify(subscriptionRepository, times(1)).save(subscription);
    }

    @Test
    @DisplayName("Should throw an error and not save when the subscription does not exist")
    void shouldThrowErrorAndNotSaveWhenSubscriptionIsNotFound() {
        // Arrange
        UpdateDeliveryPreferencesCommand command = buildValidCommand(SubscriptionId.generate());
        when(subscriptionRepository.findById(command.subscriptionId())).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> handler.handle(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Subscription not found: " + command.subscriptionId());

        verify(subscriptionRepository, times(1)).findById(command.subscriptionId());
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should propagate DomainException VO-003 and not save when the street is blank")
    void shouldPropagateDomainExceptionAndNotSaveWhenStreetIsBlank() {
        // Arrange
        Subscription subscription = buildSubscription();
        UpdateDeliveryPreferencesCommand command = new UpdateDeliveryPreferencesCommand(
                subscription.getId(), FOREIGN_ADDRESS_ID, LocalTime.of(12, 0), LocalTime.of(14, 0), "Call on arrival");
        when(subscriptionRepository.findById(command.subscriptionId())).thenReturn(Optional.of(subscription));

        // Act & Assert
        when(deliveryAddressResolver.resolve(subscription.getPatientId(), command.deliveryAddressId()))
                .thenThrow(new DomainException("ADR-001", "Delivery address is not available for the patient"));
        assertThatThrownBy(() -> handler.handle(command)).isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("code", "ADR-001");

        assertThat(subscription.getPreferences().primaryAddress().street()).isEqualTo("Main Street");
        verify(subscriptionRepository, times(1)).findById(command.subscriptionId());
        verify(subscriptionRepository, never()).save(any());
    }

    private UpdateDeliveryPreferencesCommand buildValidCommand(SubscriptionId subscriptionId) {
        return new UpdateDeliveryPreferencesCommand(subscriptionId, DELIVERY_ADDRESS_ID, LocalTime.of(12, 0),
                LocalTime.of(14, 0), "Call on arrival");
    }

    private Subscription buildSubscription() {
        LocalDate startDate = LocalDate.now().plusDays(10);
        DietPlanId planId = DietPlanId.generate();
        ServiceContract contract = new ServiceContract(planId,
                com.mcalvaro.mscatering.domain.dietplan.PlanType.BASE,
                new ValidityPeriod(startDate, startDate.plusDays(14)),
                ServiceType.FULL, new BigDecimal("250.00"), "Terms accepted", Instant.parse("2025-01-01T10:00:00Z"));
        DeliveryPreferences preferences = new DeliveryPreferences(
                new DeliveryAddress(AddressId.generate(), "Main Street", "10", "Madrid", "Door A", 40.4168, -3.7038, "600000000"),
                new TimeWindow(LocalTime.of(9, 0), LocalTime.of(11, 0)), "Leave at reception");
        return Subscription.create(SubscriptionId.generate(), PatientId.generate(), planId, contract, preferences, 1);
    }
}
