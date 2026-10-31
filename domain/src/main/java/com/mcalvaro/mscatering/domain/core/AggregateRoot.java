package com.mcalvaro.mscatering.domain.core;

/**
 * Marker abstract class for aggregate roots.
 * <p>
 * Extends {@link Entity} to inherit identity and domain-event outbox.
 * No additional state is added here; the purpose is to enforce the
 * DDD layer boundary: only {@code AggregateRoot} subclasses may be
 * referenced directly by repositories and application-layer handlers.
 */
public abstract class AggregateRoot<ID extends DomainId> extends Entity<ID> {

    protected AggregateRoot(ID id) {
        super(id);
    }
}
