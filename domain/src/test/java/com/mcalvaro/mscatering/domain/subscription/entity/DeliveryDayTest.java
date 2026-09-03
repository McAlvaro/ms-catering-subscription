package com.mcalvaro.mscatering.domain.subscription.entity;

import com.mcalvaro.mscatering.domain.core.DomainException;
import com.mcalvaro.mscatering.domain.subscription.enums.DeliveryDayStatus;
import com.mcalvaro.mscatering.domain.subscription.vo.DeliveryAddress;
import com.mcalvaro.mscatering.domain.subscription.vo.TimeWindow;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeliveryDayTest {

    @Test
    @DisplayName("Should create a scheduled delivery day with its provided details")
    void shouldCreateScheduledDeliveryDayWhenValidDetailsAreProvided() {
        // Arrange
        UUID id = UUID.randomUUID();
        LocalDate date = LocalDate.now().plusDays(4);
        DeliveryAddress address = address("Main Street", "Madrid");
        TimeWindow timeWindow = timeWindow(9, 11);

        // Act
        DeliveryDay deliveryDay = new DeliveryDay(id, date, address, timeWindow, "Leave at reception");

        // Assert
        assertThat(deliveryDay.getId()).isEqualTo(id);
        assertThat(deliveryDay.getDate()).isEqualTo(date);
        assertThat(deliveryDay.getAddress()).isEqualTo(address);
        assertThat(deliveryDay.getTimeWindow()).isEqualTo(timeWindow);
        assertThat(deliveryDay.getInstructions()).isEqualTo("Leave at reception");
        assertThat(deliveryDay.getStatus()).isEqualTo(DeliveryDayStatus.SCHEDULED);
        assertThat(deliveryDay.getConsolidatedAt()).isNull();
        assertThat(deliveryDay.getDeliveredAt()).isNull();
        assertThat(deliveryDay.getFailureReason()).isNull();
    }

    @Test
    @DisplayName("Should modify delivery details when the scheduled day is more than 48 hours away")
    void shouldModifyDetailsWhenScheduledDayIsMoreThan48HoursAway() {
        // Arrange
        DeliveryDay deliveryDay = newDeliveryDay(LocalDate.now().plusDays(4));
        DeliveryAddress newAddress = address("Oak Avenue", "Barcelona");
        TimeWindow newTimeWindow = timeWindow(14, 16);

        // Act
        deliveryDay.modify(newAddress, newTimeWindow, "Call on arrival");

        // Assert
        assertThat(deliveryDay.getAddress()).isEqualTo(newAddress);
        assertThat(deliveryDay.getTimeWindow()).isEqualTo(newTimeWindow);
        assertThat(deliveryDay.getInstructions()).isEqualTo("Call on arrival");
        assertThat(deliveryDay.getStatus()).isEqualTo(DeliveryDayStatus.SCHEDULED);
    }

    @Test
    @DisplayName("Should reject modifications within 48 hours and preserve delivery details")
    void shouldThrowDomainExceptionWhenModificationIsWithin48Hours() {
        // Arrange
        DeliveryAddress originalAddress = address("Main Street", "Madrid");
        TimeWindow originalTimeWindow = timeWindow(9, 11);
        DeliveryDay deliveryDay = new DeliveryDay(UUID.randomUUID(), LocalDate.now().plusDays(1), originalAddress,
                originalTimeWindow, "Original instructions");

        // Act & Assert
        assertThatThrownBy(() -> deliveryDay.modify(address("Oak Avenue", "Barcelona"), timeWindow(14, 16), "Changed"))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("code", "SUB-002");
        assertThat(deliveryDay.getAddress()).isEqualTo(originalAddress);
        assertThat(deliveryDay.getTimeWindow()).isEqualTo(originalTimeWindow);
        assertThat(deliveryDay.getInstructions()).isEqualTo("Original instructions");
        assertThat(deliveryDay.getStatus()).isEqualTo(DeliveryDayStatus.SCHEDULED);
    }

    @ParameterizedTest
    @EnumSource(value = DeliveryDayStatus.class, names = {"CONSOLIDATED", "DELIVERED", "FAILED"})
    @DisplayName("Should reject modifications from statuses that block delivery changes")
    void shouldThrowDomainExceptionWhenModificationIsBlockedByStatus(DeliveryDayStatus status) {
        // Arrange
        DeliveryAddress originalAddress = address("Main Street", "Madrid");
        TimeWindow originalTimeWindow = timeWindow(9, 11);
        DeliveryDay deliveryDay = deliveryDayWithStatus(status, originalAddress, originalTimeWindow);

        // Act & Assert
        assertThatThrownBy(() -> deliveryDay.modify(address("Oak Avenue", "Barcelona"), timeWindow(14, 16), "Changed"))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("code", "SUB-011");
        assertThat(deliveryDay.getAddress()).isEqualTo(originalAddress);
        assertThat(deliveryDay.getTimeWindow()).isEqualTo(originalTimeWindow);
        assertThat(deliveryDay.getInstructions()).isEqualTo("Leave at reception");
        assertThat(deliveryDay.getStatus()).isEqualTo(status);
    }

    @Test
    @DisplayName("Should reject modifications from a paused delivery day and preserve its details")
    void shouldThrowDomainExceptionWhenModificationIsRequestedForPausedDay() {
        // Arrange
        DeliveryAddress originalAddress = address("Main Street", "Madrid");
        TimeWindow originalTimeWindow = timeWindow(9, 11);
        DeliveryDay deliveryDay = new DeliveryDay(UUID.randomUUID(), LocalDate.now().plusDays(4), originalAddress,
                originalTimeWindow, "Leave at reception");
        deliveryDay.pause();

        // Act & Assert
        assertThatThrownBy(() -> deliveryDay.modify(address("Oak Avenue", "Barcelona"), timeWindow(14, 16), "Changed"))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("code", "SUB-002");
        assertThat(deliveryDay.getAddress()).isEqualTo(originalAddress);
        assertThat(deliveryDay.getTimeWindow()).isEqualTo(originalTimeWindow);
        assertThat(deliveryDay.getInstructions()).isEqualTo("Leave at reception");
        assertThat(deliveryDay.getStatus()).isEqualTo(DeliveryDayStatus.PAUSED);
    }

    @Test
    @DisplayName("Should mark a delivery day as delivered and record the delivery instant")
    void shouldMarkAsDeliveredWhenDeliveryIsCompleted() {
        // Arrange
        DeliveryDay deliveryDay = newDeliveryDay(LocalDate.now().plusDays(4));
        Instant beforeDelivery = Instant.now();

        // Act
        deliveryDay.markAsDelivered();
        Instant afterDelivery = Instant.now();

        // Assert
        assertThat(deliveryDay.getStatus()).isEqualTo(DeliveryDayStatus.DELIVERED);
        assertThat(deliveryDay.getDeliveredAt()).isBetween(beforeDelivery, afterDelivery);
        assertThat(deliveryDay.getConsolidatedAt()).isNull();
        assertThat(deliveryDay.getFailureReason()).isNull();
    }

    @Test
    @DisplayName("Should mark a delivery day as failed and retain the failure reason")
    void shouldMarkAsFailedWhenFailureReasonIsProvided() {
        // Arrange
        DeliveryDay deliveryDay = newDeliveryDay(LocalDate.now().plusDays(4));

        // Act
        deliveryDay.markAsFailed("Recipient unavailable");

        // Assert
        assertThat(deliveryDay.getStatus()).isEqualTo(DeliveryDayStatus.FAILED);
        assertThat(deliveryDay.getFailureReason()).isEqualTo("Recipient unavailable");
        assertThat(deliveryDay.getDeliveredAt()).isNull();
        assertThat(deliveryDay.getConsolidatedAt()).isNull();
    }

    @Test
    @DisplayName("Should transition delivery days through no delivery, pause, reactivation and cancellation")
    void shouldTransitionStatusesWhenDeliveryDayOperationsAreApplied() {
        // Arrange
        DeliveryDay noDeliveryDay = newDeliveryDay(LocalDate.now().plusDays(4));
        DeliveryDay pausedDay = newDeliveryDay(LocalDate.now().plusDays(4));
        DeliveryDay cancelledDay = newDeliveryDay(LocalDate.now().plusDays(4));

        // Act
        noDeliveryDay.markAsNoDelivery();
        pausedDay.pause();
        pausedDay.reactivate();
        cancelledDay.cancel();

        // Assert
        assertThat(noDeliveryDay.getStatus()).isEqualTo(DeliveryDayStatus.NOT_DELIVERED);
        assertThat(pausedDay.getStatus()).isEqualTo(DeliveryDayStatus.SCHEDULED);
        assertThat(cancelledDay.getStatus()).isEqualTo(DeliveryDayStatus.CANCELLED);
    }

    @Test
    @DisplayName("Should mark a delivery day as consolidated and record the consolidation instant")
    void shouldMarkAsConsolidatedWhenDayIsIncludedInConsolidation() {
        // Arrange
        DeliveryDay deliveryDay = newDeliveryDay(LocalDate.now().plusDays(4));
        Instant beforeConsolidation = Instant.now();

        // Act
        deliveryDay.markAsConsolidated();
        Instant afterConsolidation = Instant.now();

        // Assert
        assertThat(deliveryDay.getStatus()).isEqualTo(DeliveryDayStatus.CONSOLIDATED);
        assertThat(deliveryDay.getConsolidatedAt()).isBetween(beforeConsolidation, afterConsolidation);
        assertThat(deliveryDay.getDeliveredAt()).isNull();
        assertThat(deliveryDay.getFailureReason()).isNull();
    }

    @Test
    @DisplayName("Should reject a null identifier through the inherited entity validation")
    void shouldThrowNullPointerExceptionWhenIdIsNull() {
        // Arrange
        LocalDate date = LocalDate.now().plusDays(4);

        // Act & Assert
        assertThatThrownBy(() -> new DeliveryDay(null, date, address("Main Street", "Madrid"), timeWindow(9, 11),
                "Leave at reception"))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Entity id must not be null");
    }

    private DeliveryDay newDeliveryDay(LocalDate date) {
        return new DeliveryDay(UUID.randomUUID(), date, address("Main Street", "Madrid"), timeWindow(9, 11),
                "Leave at reception");
    }

    private DeliveryDay deliveryDayWithStatus(DeliveryDayStatus status, DeliveryAddress address, TimeWindow timeWindow) {
        DeliveryDay deliveryDay = new DeliveryDay(UUID.randomUUID(), LocalDate.now().plusDays(4), address, timeWindow,
                "Leave at reception");
        switch (status) {
            case CONSOLIDATED -> deliveryDay.markAsConsolidated();
            case DELIVERED -> deliveryDay.markAsDelivered();
            case FAILED -> deliveryDay.markAsFailed("Recipient unavailable");
            default -> throw new IllegalArgumentException("Unsupported status: " + status);
        }
        return deliveryDay;
    }

    private DeliveryAddress address(String street, String city) {
        return new DeliveryAddress(street, "10", city, "Door A", 40.4168, -3.7038, "600000000");
    }

    private TimeWindow timeWindow(int startHour, int endHour) {
        return new TimeWindow(LocalTime.of(startHour, 0), LocalTime.of(endHour, 0));
    }
}
