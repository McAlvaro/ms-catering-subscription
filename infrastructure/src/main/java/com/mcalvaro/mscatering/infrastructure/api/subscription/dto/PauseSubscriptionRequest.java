package com.mcalvaro.mscatering.infrastructure.api.subscription.dto;

import java.time.LocalDate;

public record PauseSubscriptionRequest(
        LocalDate startDate,
        LocalDate endDate,
        String reason
) {
}
