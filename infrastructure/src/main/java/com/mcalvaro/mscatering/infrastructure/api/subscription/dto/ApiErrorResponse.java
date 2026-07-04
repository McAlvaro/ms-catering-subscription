package com.mcalvaro.mscatering.infrastructure.api.subscription.dto;

public record ApiErrorResponse(
        String code,
        String message
) {
}
