package com.mcalvaro.mscatering.application.subscription.GetSubscriptionDetails;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetSubscriptionDetailsQueryHandlerTest {

    @Mock
    private ISubscriptionQueryService queryService;

    @InjectMocks
    private GetSubscriptionDetailsQueryHandler handler;

    @Test
    @DisplayName("Should return the exact subscription details when the query service finds them")
    void shouldReturnExactSubscriptionDetailsWhenQueryServiceFindsThem() {
        // Arrange
        UUID subscriptionId = UUID.randomUUID();
        GetSubscriptionDetailsQuery query = new GetSubscriptionDetailsQuery(subscriptionId);
        SubscriptionDetailsDto expected = new SubscriptionDetailsDto(subscriptionId, UUID.randomUUID(), UUID.randomUUID(), "2026-0001", "ACTIVE", LocalDate.of(2099, 1, 1), LocalDate.of(2099, 1, 15), "FULL", new BigDecimal("250.00"), List.of(), List.of());
        when(queryService.getSubscriptionDetails(subscriptionId)).thenReturn(Optional.of(expected));

        // Act
        SubscriptionDetailsDto result = handler.handle(query);

        // Assert
        assertThat(result).isSameAs(expected).isEqualTo(expected);
        verify(queryService, times(1)).getSubscriptionDetails(subscriptionId);
    }

    @Test
    @DisplayName("Should throw an exception when the query service does not find the subscription")
    void shouldThrowExceptionWhenQueryServiceDoesNotFindSubscription() {
        // Arrange
        UUID subscriptionId = UUID.randomUUID();
        GetSubscriptionDetailsQuery query = new GetSubscriptionDetailsQuery(subscriptionId);
        when(queryService.getSubscriptionDetails(subscriptionId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> handler.handle(query)).isInstanceOf(IllegalArgumentException.class).hasMessage("Subscription not found: " + subscriptionId);
        verify(queryService, times(1)).getSubscriptionDetails(subscriptionId);
    }
}
