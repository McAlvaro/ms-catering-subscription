package com.mcalvaro.mscatering.domain.core;

/**
 * A domain event whose business flow can be traced across bounded contexts.
 */
public interface CorrelatedDomainEvent extends DomainEvent {

    String correlationId();
}
