package com.mcalvaro.mscatering.domain.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Abstract base class for all domain entities.
 * <p>
 * Holds the entity identity ({@code UUID}) and an internal outbox of
 * {@link DomainEvent}s that are queued during state mutations and
 * dispatched by the infrastructure layer (Unit of Work) after the
 * aggregate is persisted.
 */
public abstract class Entity {

    private final UUID id;
    private final List<DomainEvent> domainEvents = new ArrayList<>();

    protected Entity(UUID id) {
        Objects.requireNonNull(id, "Entity id must not be null");
        this.id = id;
    }

    public UUID getId() {
        return id;
    }

    protected void addDomainEvent(DomainEvent event) {
        Objects.requireNonNull(event, "Domain event must not be null");
        domainEvents.add(event);
    }

    public void clearDomainEvents() {
        domainEvents.clear();
    }

    public List<DomainEvent> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Entity other))
            return false;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
