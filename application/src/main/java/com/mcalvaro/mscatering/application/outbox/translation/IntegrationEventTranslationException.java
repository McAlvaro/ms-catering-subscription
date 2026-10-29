package com.mcalvaro.mscatering.application.outbox.translation;

public final class IntegrationEventTranslationException extends RuntimeException {
    public IntegrationEventTranslationException(String message) {
        super(message);
    }
}
