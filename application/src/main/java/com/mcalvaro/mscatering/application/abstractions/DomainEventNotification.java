package com.mcalvaro.mscatering.application.abstractions;

import an.awesome.pipelinr.Notification;
import com.mcalvaro.mscatering.domain.core.DomainEvent;

/**
 * Envoltorio (Wrapper) para permitir que los DomainEvents (que pertenecen al dominio puro)
 * sean despachados a través del bus de Pipelinr (que pertenece a la capa de Aplicación).
 */
public record DomainEventNotification(DomainEvent domainEvent) implements Notification {
}
